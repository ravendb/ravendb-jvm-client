package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.GetTcpInfoForRemoteTaskCommand;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.exceptions.ClientVersionMismatchException;
import net.ravendb.client.exceptions.cluster.NodeIsPassiveException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.exceptions.documents.subscriptions.*;
import net.ravendb.client.exceptions.security.AuthorizationException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.*;
import net.ravendb.client.serverwide.commands.GetTcpInfoCommand;
import net.ravendb.client.serverwide.commands.TcpConnectionInfo;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderResponse;
import net.ravendb.client.serverwide.tcp.TcpNegotiateParameters;
import net.ravendb.client.serverwide.tcp.TcpNegotiation;
import net.ravendb.client.util.TcpUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SubscriptionWorker<T> implements CleanCloseable {

    private final Class<T> _clazz;
    private final boolean _revisions;
    private final Log _logger;
    private final DocumentStore _store;
    private final String _dbName;
    private final CancellationTokenSource _processingCts = new CancellationTokenSource();
    private final SubscriptionWorkerOptions _options;
    private Consumer<SubscriptionBatch<T>> _subscriber;
    private Socket _tcpClient;
    private JsonParser _parser;
    private boolean _disposed;
    private CompletableFuture<Void> _subscriptionTask;
    private int _forcedTopologyUpdateAttempts = 0;

    private List<Consumer<SubscriptionBatch<T>>> afterAcknowledgment;
    private List<Consumer<Exception>> onSubscriptionConnectionRetry;
    private List<Consumer<Exception>> onUnexpectedSubscriptionError;

    public void addAfterAcknowledgmentListener(Consumer<SubscriptionBatch<T>> handler) {
        afterAcknowledgment.add(handler);
    }

    public void removeAfterAcknowledgmentListener(Consumer<SubscriptionBatch<T>> handler) {
        afterAcknowledgment.remove(handler);
    }

    public void addOnSubscriptionConnectionRetry(Consumer<Exception> handler) {
        onSubscriptionConnectionRetry.add(handler);
    }

    public void removeOnSubscriptionConnectionRetry(Consumer<Exception> handler) {
        onSubscriptionConnectionRetry.remove(handler);
    }

    public void addOnUnexpectedSubscriptionError(Consumer<Exception> handler) {
        onUnexpectedSubscriptionError.add(handler);
    }

    public void removeOnUnexpectedSubscriptionError(Consumer<Exception> handler) {
        onUnexpectedSubscriptionError.remove(handler);
    }

    @SuppressWarnings("unchecked")
    SubscriptionWorker(Class<?> clazz, SubscriptionWorkerOptions options, boolean withRevisions, DocumentStore documentStore, String dbName) {
        _clazz = (Class<T>) clazz;
        _options = options;
        _revisions = withRevisions;

        if (StringUtils.isEmpty(options.getSubscriptionName())) {
            throw new IllegalArgumentException("SubscriptionConnectionOptions must specify the subscriptionName");
        }
        _store = documentStore;
        _dbName = documentStore.getEffectiveDatabase(dbName);
        _logger = LogFactory.getLog(SubscriptionWorker.class);

        afterAcknowledgment = new ArrayList<>();
        onSubscriptionConnectionRetry = new ArrayList<>();
    }

    @Override
    public void close() {
        close(true);
    }

    public void close(boolean waitForSubscriptionTask) {
        if (_disposed) {
            return;
        }
        try {
            _disposed = true;
            _processingCts.cancel();


            closeTcpClient(); // we disconnect immediately

            if (_subscriptionTask != null && waitForSubscriptionTask) {
                try {
                    _subscriptionTask.get();
                } catch (Exception e) {
                    // just need to wait for it to end
                }
            }

            if (_subscriptionLocalRequestExecutor != null) {
                _subscriptionLocalRequestExecutor.close();
            }
        } catch (Exception ex) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("Error during close of subscription: " + ex.getMessage(), ex);
            }
        } finally {
            if (onClosed != null) {
                onClosed.accept(this);
            }
        }
    }


    public CompletableFuture<Void> run(Consumer<SubscriptionBatch<T>> processDocuments) {
        if (processDocuments == null) {
            throw new IllegalArgumentException("ProcessDocuments cannot be null");
        }
        _subscriber = processDocuments;
        return run();
    }

    private CompletableFuture<Void> run() {
        if (_subscriptionTask != null) {
            throw new IllegalStateException("The subscription is already running");
        }

        return _subscriptionTask = runSubscriptionAsync();
    }

    private ServerNode _redirectNode;
    private RequestExecutor _subscriptionLocalRequestExecutor;

    public String getCurrentNodeTag() {
        if (_redirectNode != null) {
            return _redirectNode.getClusterTag();
        }
        return null;
    }

    public String getSubscriptionName() {
        if (_options != null) {
            return _options.getSubscriptionName();
        }
        return null;
    }

    private Socket connectToServer() throws IOException, GeneralSecurityException {
        GetTcpInfoForRemoteTaskCommand command = new GetTcpInfoForRemoteTaskCommand(
                "Subscription/" + _dbName,
                _dbName,
                _options != null ? _options.getSubscriptionName() : null,
                true);

        RequestExecutor requestExecutor = _store.getRequestExecutor(_dbName);

        TcpConnectionInfo tcpInfo;

        if (_redirectNode != null) {
            try {
                requestExecutor.execute(_redirectNode, null, command, false, null);
                tcpInfo = command.getResult();
            } catch (ClientVersionMismatchException e) {
                tcpInfo = legacyTryGetTcpInfo(requestExecutor, _redirectNode);
            } catch (Exception e) {
                // if we failed to talk to a node, we'll forget about it and let the topology to
                // redirect us to the current node

                _redirectNode = null;
                throw new RuntimeException(e);
            }
        } else {
            try {
                requestExecutor.execute(command);
                tcpInfo = command.getResult();

                if (tcpInfo.getNodeTag() != null) {
                    TcpConnectionInfo finalTcpInfo = tcpInfo;
                    _redirectNode = requestExecutor.getTopology().getNodes()
                            .stream()
                            .filter(x -> finalTcpInfo.getNodeTag().equals(x.getClusterTag()))
                            .findFirst()
                            .orElse(null);
                }
            } catch (ClientVersionMismatchException e) {
                tcpInfo = legacyTryGetTcpInfo(requestExecutor);
            }
        }

        Tuple<Socket, String> socketStringTuple = TcpUtils.connectWithPriority(tcpInfo, command.getResult().getCertificate(), _store.getCertificate(), _store.getCertificatePrivateKeyPassword());
        _tcpClient = socketStringTuple.first;
        String chosenUrl = socketStringTuple.second;
        _tcpClient.setTcpNoDelay(true);
        _tcpClient.setSendBufferSize(_options.getSendBufferSize());
        _tcpClient.setReceiveBufferSize(_options.getReceiveBufferSize());

        String databaseName = _store.getEffectiveDatabase(_dbName);

        TcpNegotiateParameters parameters = new TcpNegotiateParameters();
        parameters.setDatabase(databaseName);
        parameters.setOperation(TcpConnectionHeaderMessage.OperationTypes.SUBSCRIPTION);
        parameters.setVersion(TcpConnectionHeaderMessage.SUBSCRIPTION_TCP_VERSION);
        parameters.setReadResponseAndGetVersionCallback(this::readServerResponseAndGetVersion);
        parameters.setDestinationNodeTag(getCurrentNodeTag());
        parameters.setDestinationUrl(chosenUrl);

        _supportedFeatures = TcpNegotiation.negotiateProtocolVersion(_tcpClient.getOutputStream(), parameters);

        if (_supportedFeatures.protocolVersion <= 0) {
            throw new IllegalStateException(_options.getSubscriptionName() + " : TCP negotiation resulted with an invalid protocol version: " + _supportedFeatures.protocolVersion);
        }

        byte[] options = JsonExtensions.writeValueAsBytes(_options);

        _tcpClient.getOutputStream().write(options);
        _tcpClient.getOutputStream().flush();

        if (_subscriptionLocalRequestExecutor != null) {
            _subscriptionLocalRequestExecutor.close();
        }
        _subscriptionLocalRequestExecutor = RequestExecutor.createForSingleNodeWithoutConfigurationUpdates(
                command.getRequestedNode().getUrl(),
                _dbName, requestExecutor.getCertificate(), requestExecutor.getKeyPassword(), requestExecutor.getTrustStore(),
                _store.getExecutorService(),
                _store.getConventions());

        _store.registerEvents(_subscriptionLocalRequestExecutor);

        return _tcpClient;
    }

    private TcpConnectionInfo legacyTryGetTcpInfo(RequestExecutor requestExecutor) {
        GetTcpInfoCommand tcpCommand = new GetTcpInfoCommand("Subscription/" + _dbName, _dbName);
        try {
            requestExecutor.execute(tcpCommand, null);
        } catch (Exception e) {
            _redirectNode = null;
            throw e;
        }

        return tcpCommand.getResult();
    }

    private TcpConnectionInfo legacyTryGetTcpInfo(RequestExecutor requestExecutor, ServerNode node) {
        GetTcpInfoCommand tcpCommand = new GetTcpInfoCommand("Subscription/" + _dbName, _dbName);

        try {
            requestExecutor.execute(node, null, tcpCommand, false, null);
        } catch (Exception e) {
            _redirectNode = null;
            throw e;
        }

        return tcpCommand.getResult();
    }

    private void ensureParser() throws IOException {
        if (_parser == null) {
            _parser = JsonExtensions.getDefaultMapper().getFactory().createParser(_tcpClient.getInputStream());
            _parser.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        }
    }

    private int readServerResponseAndGetVersion(String url) {
        try {
            //Reading reply from server
            ensureParser();
            TreeNode response = _parser.readValueAsTree();
            TcpConnectionHeaderResponse reply = JsonExtensions.getDefaultMapper().treeToValue(response, TcpConnectionHeaderResponse.class);

            switch (reply.getStatus()) {
                case OK:
                    return reply.getVersion();
                case AUTHORIZATION_FAILED:
                    throw new AuthorizationException("Cannot access database " + _dbName + " because " + reply.getMessage());
                case TCP_VERSION_MISMATCH:
                    if (reply.getVersion() != TcpNegotiation.OUT_OF_RANGE_STATUS) {
                        return reply.getVersion();
                    }
                    //Kindly request the server to drop the connection
                    sendDropMessage(reply);
                    throw new IllegalStateException("Can't connect to database " + _dbName + " because: " + reply.getMessage());
            }
            return reply.getVersion();
        } catch (IOException e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }

    private void sendDropMessage(TcpConnectionHeaderResponse reply) throws IOException {
        TcpConnectionHeaderMessage dropMsg = new TcpConnectionHeaderMessage();
        dropMsg.setOperation(TcpConnectionHeaderMessage.OperationTypes.DROP);
        dropMsg.setDatabaseName(_dbName);
        dropMsg.setOperationVersion(TcpConnectionHeaderMessage.SUBSCRIPTION_TCP_VERSION);
        dropMsg.setInfo("Couldn't agree on subscription tcp version ours: " + TcpConnectionHeaderMessage.SUBSCRIPTION_TCP_VERSION + " theirs: " + reply.getVersion());

        byte[] header = JsonExtensions.writeValueAsBytes(dropMsg);
        _tcpClient.getOutputStream().write(header);
        _tcpClient.getOutputStream().flush();
    }

    private void assertConnectionState(SubscriptionConnectionServerMessage connectionStatus) {
        if (connectionStatus.getType() == SubscriptionConnectionServerMessage.MessageType.ERROR) {
            if (connectionStatus.getException().contains("DatabaseDoesNotExistException")) {
                throw new DatabaseDoesNotExistException(_dbName + " does not exists. " + connectionStatus.getMessage());
            }
        }

        if (connectionStatus.getType() != SubscriptionConnectionServerMessage.MessageType.CONNECTION_STATUS) {
            throw new IllegalStateException("Server returned illegal type message when expecting connection status, was:" + connectionStatus.getType());
        }

        switch (connectionStatus.getStatus()) {
            case ACCEPTED:
                break;
            case IN_USE:
                throw new SubscriptionInUseException("Subscription with id " + _options.getSubscriptionName() + " cannot be opened, because it's in use and the connection strategy is " + _options.getStrategy());
            case CLOSED:
                boolean canReconnect = false;
                JsonNode canReconnectNode = connectionStatus.getData().get("CanReconnect");
                if (canReconnectNode != null && canReconnectNode.isBoolean() && canReconnectNode.asBoolean()) {
                    canReconnect = true;
                }
                throw new SubscriptionClosedException("Subscription with id " + _options.getSubscriptionName() + " was closed. " + connectionStatus.getException(), canReconnect);
            case INVALID:
                throw new SubscriptionInvalidStateException("Subscription with id " + _options.getSubscriptionName() + " cannot be opened, because it is in invalid state. " + connectionStatus.getException());
            case NOT_FOUND:
                throw new SubscriptionDoesNotExistException("Subscription with id " + _options.getSubscriptionName() + " cannot be opened, because it does not exist. " + connectionStatus.getException());
            case REDIRECT:
                ObjectNode data = connectionStatus.getData();
                String appropriateNode = data.get("RedirectedTag").asText();
                JsonNode currentTagRaw = data.get("CurrentTag");
                String currentNode = currentTagRaw != null && !currentTagRaw.isNull() ? currentTagRaw.asText() : null;
                JsonNode rawReasons = data.get("Reasons");
                Map<String, String> reasonsDictionary = new HashMap<>();
                if (rawReasons instanceof ArrayNode) {
                    ArrayNode rawReasonsArray = (ArrayNode) rawReasons;
                    for (JsonNode item : rawReasonsArray) {
                        if (item instanceof ObjectNode) {
                            ObjectNode itemAsBlittable = (ObjectNode) item;

                            if (itemAsBlittable.size() == 1) {
                                String tagName = itemAsBlittable.fieldNames().next();
                                reasonsDictionary.put(tagName, itemAsBlittable.get(tagName).asText());
                            }
                        }
                    }
                }

                String reasonsJoined = reasonsDictionary
                        .entrySet()
                        .stream()
                        .map(x -> x.getKey() + ":" + x.getValue())
                        .collect(Collectors.joining());

                SubscriptionDoesNotBelongToNodeException notBelongToNodeException =
                        new SubscriptionDoesNotBelongToNodeException(
                                "Subscription with id '" + _options.getSubscriptionName() + "' cannot be processed by current node '" + currentNode + "', " +
                                        "it will be redirected to " + appropriateNode + System.lineSeparator() + reasonsJoined);
                notBelongToNodeException.setAppropriateNode(appropriateNode);
                notBelongToNodeException.setReasons(reasonsDictionary);
                throw notBelongToNodeException;
            case CONCURRENCY_RECONNECT:
                throw new SubscriptionChangeVectorUpdateConcurrencyException(connectionStatus.getMessage());
            default:
                throw new IllegalStateException("Subscription " + _options.getSubscriptionName() + " could not be opened, reason: " + connectionStatus.getStatus());
        }

    }

    @SuppressWarnings("ConstantConditions")
    private void processSubscription() throws Exception {
        try {
            _processingCts.getToken().throwIfCancellationRequested();

            try (Socket socket = connectToServer()) {
                _processingCts.getToken().throwIfCancellationRequested();

                Socket tcpClientCopy = _tcpClient;

                SubscriptionConnectionServerMessage connectionStatus = readNextObject(tcpClientCopy);
                if (_processingCts.getToken().isCancellationRequested()) {
                    return;
                }

                if (connectionStatus.getType() != SubscriptionConnectionServerMessage.MessageType.CONNECTION_STATUS
                        || connectionStatus.getStatus() != SubscriptionConnectionServerMessage.ConnectionStatus.ACCEPTED) {
                    assertConnectionState(connectionStatus);
                }

                _lastConnectionFailure = null;
                if (_processingCts.getToken().isCancellationRequested()) {
                    return;
                }

                CompletableFuture<Void> notifiedSubscriber = CompletableFuture.completedFuture(null);

                SubscriptionBatch<T> batch = new SubscriptionBatch<>(_clazz, _revisions, _subscriptionLocalRequestExecutor, _store, _dbName, _logger);

                while (!_processingCts.getToken().isCancellationRequested()) {
                    // start reading next batch from server on 1'st thread (can be before client started processing)
                    CompletableFuture<BatchFromServer> readFromServer =
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    return readSingleSubscriptionBatchFromServer(tcpClientCopy, batch);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }, _store.getExecutorService());

                    try {
                        notifiedSubscriber.get();
                    } catch (Exception e) {
                        // if the subscriber errored, we shut down
                        try {
                            closeTcpClient();
                        } catch (Exception ex2) {
                            // nothing to be done here
                        }

                        throw e;
                    }

                    BatchFromServer incomingBatch = readFromServer.get();

                    _processingCts.getToken().throwIfCancellationRequested();

                    String lastReceivedChangeVector = batch.initialize(incomingBatch);

                    notifiedSubscriber = CompletableFuture.runAsync(() -> {
                        try {
                            _subscriber.accept(batch);
                        } catch (Exception ex) {
                            if (_logger.isDebugEnabled()) {
                                _logger.debug("Subscription " + _options.getSubscriptionName() + ". Subscriber threw an exception on document batch", ex);
                            }

                            if (!_options.isIgnoreSubscriberErrors()) {
                                throw new SubscriberErrorException("Subscriber threw an exception in subscription " + _options.getSubscriptionName(), ex);
                            }
                        }

                        try {
                            if (tcpClientCopy != null) {
                                sendAck(lastReceivedChangeVector, tcpClientCopy);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, _store.getExecutorService());
                }
            }

        } catch (OperationCancelledException e) {
            if (!_disposed) {
                throw e;
            }

            // otherwise this is thrown when shutting down,
            // it isn't an error, so we don't need to treat it as such
        }
    }

    private BatchFromServer readSingleSubscriptionBatchFromServer(Socket socket, SubscriptionBatch<T> batch) throws IOException {
        List<SubscriptionConnectionServerMessage> incomingBatch = new ArrayList<>();
        List<ObjectNode> includes = new ArrayList<>();
        List<BatchFromServer.CounterIncludeItem> counterIncludes = new ArrayList<>();
        List<ObjectNode> timeSeriesIncludes = new ArrayList<>();
        boolean endOfBatch = false;
        while (!endOfBatch && !_processingCts.getToken().isCancellationRequested()) {
            SubscriptionConnectionServerMessage receivedMessage = readNextObject(socket);
            if (receivedMessage == null || _processingCts.getToken().isCancellationRequested()) {
                break;
            }

            switch (receivedMessage.getType()) {
                case DATA:
                    incomingBatch.add(receivedMessage);
                    break;
                case INCLUDES:
                    includes.add(receivedMessage.getIncludes());
                    break;
                case COUNTER_INCLUDES:
                    counterIncludes.add(new BatchFromServer.CounterIncludeItem(receivedMessage.getCounterIncludes(), receivedMessage.getIncludedCounterNames()));
                    break;
                case TIME_SERIES_INCLUDES:
                    timeSeriesIncludes.add(receivedMessage.getTimeSeriesIncludes());
                    break;
                case END_OF_BATCH:
                    endOfBatch = true;
                    break;
                case CONFIRM:
                    EventHelper.invoke(afterAcknowledgment, batch);

                    incomingBatch.clear();
                    batch.getItems().clear();
                    break;
                case CONNECTION_STATUS:
                    assertConnectionState(receivedMessage);
                    break;
                case ERROR:
                    throwSubscriptionError(receivedMessage);
                    break;
                default:
                    throwInvalidServerResponse(receivedMessage);
                    break;
            }
        }

        BatchFromServer batchFromServer = new BatchFromServer();
        batchFromServer.setMessages(incomingBatch);
        batchFromServer.setIncludes(includes);
        batchFromServer.setCounterIncludes(counterIncludes);
        batchFromServer.setTimeSeriesIncludes(timeSeriesIncludes);
        return batchFromServer;
    }

    private static void throwInvalidServerResponse(SubscriptionConnectionServerMessage receivedMessage) {
        throw new IllegalArgumentException("Unrecognized message " + receivedMessage.getType() + " type received from server");
    }

    private static void throwSubscriptionError(SubscriptionConnectionServerMessage receivedMessage) {
        throw new IllegalStateException("Connected terminated by server. Exception: " + ObjectUtils.firstNonNull(receivedMessage.getException(), "None"));
    }

    private SubscriptionConnectionServerMessage readNextObject(Socket socket) throws IOException {
        if (_processingCts.getToken().isCancellationRequested() || !_tcpClient.isConnected()) {
            return null;
        }

        if (_disposed) { //if we are disposed, nothing to do...
            return null;
        }

        TreeNode response = _parser.readValueAsTree();
        return JsonExtensions.getDefaultMapper().treeToValue(response, SubscriptionConnectionServerMessage.class);
    }

    private void sendAck(String lastReceivedChangeVector, Socket networkStream) throws IOException {
        SubscriptionConnectionClientMessage msg = new SubscriptionConnectionClientMessage();
        msg.setChangeVector(lastReceivedChangeVector);
        msg.setType(SubscriptionConnectionClientMessage.MessageType.ACKNOWLEDGE);

        byte[] ack = JsonExtensions.writeValueAsBytes(msg);
        networkStream.getOutputStream().write(ack);
        networkStream.getOutputStream().flush();
    }

    private CompletableFuture<Void> runSubscriptionAsync() {
        return CompletableFuture.runAsync(() -> {
            while (!_processingCts.getToken().isCancellationRequested()) {
                try {
                    closeTcpClient();
                    if (_logger.isInfoEnabled()) {
                        _logger.info("Subscription " + _options.getSubscriptionName() + ". Connecting to server...");
                    }

                    processSubscription();
                } catch (Exception ex) {
                    try {
                        if (_processingCts.getToken().isCancellationRequested()) {
                            if (!_disposed) {
                                throw ex;
                            }
                            return;
                        }

                        if (_logger.isInfoEnabled()) {
                            _logger.info("Subscription " + _options.getSubscriptionName() + ". Pulling task threw the following exception", ex);
                        }

                        if (shouldTryToReconnect(ex)) {
                            Thread.sleep(_options.getTimeToWaitBeforeConnectionRetry().toMillis());

                            if (_redirectNode == null) {
                                RequestExecutor reqEx = _store.getRequestExecutor(_dbName);
                                List<ServerNode> curTopology = reqEx.getTopologyNodes();
                                int nextNodeIndex = (_forcedTopologyUpdateAttempts++) % curTopology.size();
                                try {
                                    _redirectNode = curTopology.get(nextNodeIndex);
                                    if (_logger.isInfoEnabled()) {
                                        _logger.info("Subscription '" + _options.getSubscriptionName() + "'. Will modify redirect node from null to " + _redirectNode.getClusterTag(), ex);
                                    }
                                } catch (Exception e) {
                                    // will let topology to decide
                                    if (_logger.isInfoEnabled()) {
                                        _logger.info("Subscription '" + _options.getSubscriptionName() + "'. Could not select the redirect node will keep it null.", e);
                                    }
                                }
                            }

                            EventHelper.invoke(onSubscriptionConnectionRetry, ex);
                        } else {
                            if (_logger.isErrorEnabled()) {
                                _logger.error("Connection to subscription " + _options.getSubscriptionName() + " have been shut down because of an error", ex);
                            }
                            throw ex;
                        }
                    } catch (Exception e) {
                        throw ExceptionsUtils.unwrapException(e);
                    }
                }
            }
        }, _store.getExecutorService());
    }

    private Date _lastConnectionFailure;
    private TcpConnectionHeaderMessage.SupportedFeatures _supportedFeatures;

    private void assertLastConnectionFailure() {
        if (_lastConnectionFailure == null) {
            _lastConnectionFailure = new Date();
            return;
        }

        if (new Date().getTime() - _lastConnectionFailure.getTime() > _options.getMaxErroneousPeriod().toMillis()) {
            throw new SubscriptionInvalidStateException("Subscription connection was in invalid state for more than "
                    + _options.getMaxErroneousPeriod() + " and therefore will be terminated");
        }
    }

    private boolean shouldTryToReconnect(Exception ex) {
        ex = ExceptionsUtils.unwrapException(ex);
        if (ex instanceof SubscriptionDoesNotBelongToNodeException) {
            SubscriptionDoesNotBelongToNodeException se = (SubscriptionDoesNotBelongToNodeException) ex;
            RequestExecutor requestExecutor = _store.getRequestExecutor(_dbName);

            if (se.getAppropriateNode() == null) {
                assertLastConnectionFailure();

                _redirectNode = null;
                return true;
            }

            ServerNode nodeToRedirectTo = requestExecutor.getTopologyNodes()
                    .stream()
                    .filter(x -> x.getClusterTag().equals(se.getAppropriateNode()))
                    .findFirst()
                    .orElse(null);

            if (nodeToRedirectTo == null) {
                throw new IllegalStateException("Could not redirect to " + se.getAppropriateNode() + ", because it was not found in local topology, even after retrying");
            }

            _redirectNode = nodeToRedirectTo;
            return true;
        } else if (ex instanceof NodeIsPassiveException) {
            // if we failed to talk to a node, we'll forget about it and let the topology to
            // redirect us to the current node
            _redirectNode = null;
            return true;
        } else if (ex instanceof SubscriptionChangeVectorUpdateConcurrencyException) {
            return true;
        } else if (ex instanceof SubscriptionClosedException) {
            SubscriptionClosedException sce = (SubscriptionClosedException) ex;
            if (sce.isCanReconnect()) {
                return true;
            }

            _processingCts.cancel();
            return false;
        }

        if (ex instanceof SubscriptionInUseException
                || ex instanceof SubscriptionDoesNotExistException
                || ex instanceof SubscriptionInvalidStateException
                || ex instanceof DatabaseDoesNotExistException
                || ex instanceof AuthorizationException
                || ex instanceof AllTopologyNodesDownException
                || ex instanceof SubscriberErrorException) {
            _processingCts.cancel();
            return false;
        }

        EventHelper.invoke(onUnexpectedSubscriptionError, ex);
        assertLastConnectionFailure();
        return true;
    }

    private void closeTcpClient() {
        if (_parser != null) {
            IOUtils.closeQuietly(_parser, null);
            _parser = null;
        }

        if (_tcpClient != null) {
            IOUtils.closeQuietly(_tcpClient, null);
            _tcpClient = null;
        }
    }

    Consumer<SubscriptionWorker<T>> onClosed = null;
}
