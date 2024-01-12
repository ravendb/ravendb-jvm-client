package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.GetTcpInfoForRemoteTaskCommand;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.exceptions.ClientVersionMismatchException;
import net.ravendb.client.exceptions.InvalidNetworkTopologyException;
import net.ravendb.client.exceptions.cluster.NodeIsPassiveException;
import net.ravendb.client.exceptions.database.DatabaseDisabledException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.exceptions.documents.subscriptions.*;
import net.ravendb.client.exceptions.security.AuthorizationException;
import net.ravendb.client.exceptions.sharding.NotSupportedInShardingException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.*;
import net.ravendb.client.serverwide.commands.GetTcpInfoCommand;
import net.ravendb.client.serverwide.commands.TcpConnectionInfo;
import net.ravendb.client.serverwide.tcp.*;
import net.ravendb.client.util.TcpUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractSubscriptionWorker<TBatch extends SubscriptionBatchBase<TType>, TType> implements CleanCloseable {

    private final ExecutorService _executorService;
    protected final Class<TType> _clazz;
    protected final boolean _revisions;
    protected final Log _logger;

    protected final String _dbName;
    protected final CancellationTokenSource _processingCts = new CancellationTokenSource();
    protected final SubscriptionWorkerOptions _options;
    private Consumer<TBatch> _subscriber;
    private Socket _tcpClient;
    private JsonParser _parser;
    protected boolean _disposed;
    protected CompletableFuture<Void> _subscriptionTask;
    protected int _forcedTopologyUpdateAttempts = 0;

    private final List<Consumer<TBatch>> afterAcknowledgment;
    private final List<Consumer<Void>> onEstablishedSubscriptionConnection;
    private final List<Consumer<Exception>> onSubscriptionConnectionRetry;
    private final List<Consumer<Exception>> onUnexpectedSubscriptionError;

    public String getWorkerId() {
        return _options.getWorkerId();
    }

    public void addOnEstablishedSubscriptionConnection(Consumer<Void> handler) {
        onEstablishedSubscriptionConnection.add(handler);
    }

    public void removeOnEstablishedSubscriptionConnection(Consumer<Void> handler) {
        onEstablishedSubscriptionConnection.remove(handler);
    }

    public void addAfterAcknowledgmentListener(Consumer<TBatch> handler) {
        afterAcknowledgment.add(handler);
    }

    public void removeAfterAcknowledgmentListener(Consumer<TBatch> handler) {
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
    AbstractSubscriptionWorker(Class<?> clazz, SubscriptionWorkerOptions options, boolean withRevisions, String dbName, ExecutorService executorService) {
        _clazz = (Class<TType>) clazz;
        _options = options;
        _revisions = withRevisions;

        if (StringUtils.isEmpty(options.getSubscriptionName())) {
            throw new IllegalArgumentException("SubscriptionConnectionOptions must specify the subscriptionName");
        }
        _dbName = dbName;
        _logger = LogFactory.getLog(SubscriptionWorker.class);

        afterAcknowledgment = new ArrayList<>();
        onSubscriptionConnectionRetry = new ArrayList<>();
        onEstablishedSubscriptionConnection = new ArrayList<>();
        onUnexpectedSubscriptionError = new ArrayList<>();
        _executorService = executorService;
    }

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
                    _subscriptionTask.get(60, TimeUnit.SECONDS);
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


    public CompletableFuture<Void> run(Consumer<TBatch> processDocuments) {
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
    protected RequestExecutor _subscriptionLocalRequestExecutor;

    protected Integer subscriptionTcpVersion;

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

    protected boolean shouldUseCompression() {
        boolean compressionSupport = false;
        int version = subscriptionTcpVersion != null ? subscriptionTcpVersion : TcpConnectionHeaderMessage.SUBSCRIPTION_TCP_VERSION;
        if (version >= 53_000 && !getRequestExecutor().getConventions().isDisableTcpCompression()) {
            compressionSupport = true;
        }

        return compressionSupport;
    }

    private Socket connectToServer() throws IOException, GeneralSecurityException {
        GetTcpInfoForRemoteTaskCommand command = new GetTcpInfoForRemoteTaskCommand(
                "Subscription/" + _dbName,
                _dbName,
                _options != null ? _options.getSubscriptionName() : null,
                true);

        RequestExecutor requestExecutor = getRequestExecutor();

        trySetRedirectNodeOnConnectToServer();

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

        TcpUtils.ConnectSecuredTcpSocketResult result = TcpUtils.connectSecuredTcpSocket(
                tcpInfo,
                command.getResult().getCertificate(),
                requestExecutor.getCertificate(),
                requestExecutor.getKeyPassword(),
                TcpConnectionHeaderMessage.OperationTypes.SUBSCRIPTION,
                this::negotiateProtocolVersionForSubscription
        );
        _tcpClient = result.socket;
        _tcpClient.setTcpNoDelay(true);
        _tcpClient.setSendBufferSize(_options.getSendBufferSize());
        _tcpClient.setReceiveBufferSize(_options.getReceiveBufferSize());

        _supportedFeatures = result.supportedFeatures;

        if (_supportedFeatures.protocolVersion <= 0) {
            throw new IllegalStateException(_options.getSubscriptionName() + " : TCP negotiation resulted with an invalid protocol version: " + _supportedFeatures.protocolVersion);
        }

        byte[] options = JsonExtensions.writeValueAsBytes(_options);

        _tcpClient.getOutputStream().write(options);
        _tcpClient.getOutputStream().flush();

        setLocalRequestExecutor(command.getRequestedNode().getUrl(), requestExecutor.getCertificate(), requestExecutor.getKeyPassword(), requestExecutor.getTrustStore());

        return _tcpClient;
    }

    private TcpConnectionHeaderMessage.SupportedFeatures negotiateProtocolVersionForSubscription(
            String chosenUrl,
            TcpConnectionInfo tcpInfo,
            Socket socket
    ) throws IOException {

        TcpNegotiateParameters parameters = new TcpNegotiateParameters();
        parameters.setDatabase(_dbName);
        parameters.setOperation(TcpConnectionHeaderMessage.OperationTypes.SUBSCRIPTION);
        parameters.setVersion(TcpConnectionHeaderMessage.SUBSCRIPTION_TCP_VERSION);
        parameters.setReadResponseAndGetVersionCallback(this::readServerResponseAndGetVersion);
        parameters.setDestinationNodeTag(getCurrentNodeTag());
        parameters.setDestinationUrl(chosenUrl);
        parameters.setDestinationServerId(tcpInfo.getServerId());
        parameters.setLicensedFeatures(new LicensedFeatures());
        parameters.getLicensedFeatures().setDataCompression(shouldUseCompression());

        return TcpNegotiation.negotiateProtocolVersion(socket, parameters);
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

    private void ensureParser(Socket socket) throws IOException {
        if (_parser == null) {
            _parser = JsonExtensions.getDefaultMapper().getFactory().createParser(socket.getInputStream());
            _parser.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        }
    }

    private TcpConnectionHeaderMessage.NegotiationResponse readServerResponseAndGetVersion(String url, Socket socket) {
        try {
            //Reading reply from server
            ensureParser(socket);
            TreeNode response = _parser.readValueAsTree();
            TcpConnectionHeaderResponse reply = JsonExtensions.getDefaultMapper().treeToValue(response, TcpConnectionHeaderResponse.class);

            switch (reply.getStatus()) {
                case OK: {
                    TcpConnectionHeaderMessage.NegotiationResponse result = new TcpConnectionHeaderMessage.NegotiationResponse();
                    result.version = reply.getVersion();
                    result.licensedFeatures = reply.getLicensedFeatures();
                    return result;
                }
                case AUTHORIZATION_FAILED:
                    throw new AuthorizationException("Cannot access database " + _dbName + " because " + reply.getMessage());
                case TCP_VERSION_MISMATCH:
                    if (reply.getVersion() != TcpNegotiation.OUT_OF_RANGE_STATUS) {
                        TcpConnectionHeaderMessage.NegotiationResponse result = new TcpConnectionHeaderMessage.NegotiationResponse();
                        result.version = reply.getVersion();
                        result.licensedFeatures = reply.getLicensedFeatures();
                        return result;
                    }
                    //Kindly request the server to drop the connection
                    sendDropMessage(reply);
                    throw new IllegalStateException("Can't connect to database " + _dbName + " because: " + reply.getMessage());
                case INVALID_NETWORK_TOPOLOGY:
                    throw new InvalidNetworkTopologyException("Failed to connect to url " + url + " because " + reply.getMessage());
            }
            TcpConnectionHeaderMessage.NegotiationResponse result = new TcpConnectionHeaderMessage.NegotiationResponse();
            result.version = reply.getVersion();
            result.licensedFeatures = reply.getLicensedFeatures();
            return result;
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
            } else if (connectionStatus.getException().contains("NotSupportedInShardingException")) {
                throw new NotSupportedInShardingException(connectionStatus.getMessage());
            } else if (connectionStatus.getException().contains("DatabaseDisabledException")) {
                throw new DatabaseDisabledException(connectionStatus.getMessage());
            }
        }

        if (connectionStatus.getType() != SubscriptionConnectionServerMessage.MessageType.CONNECTION_STATUS) {
            String message = "Server returned illegal type message when expecting connection status, was:" + connectionStatus.getType();

            if (connectionStatus.getType() == SubscriptionConnectionServerMessage.MessageType.ERROR) {
                message += ". Exception: " + connectionStatus.getException();
            }

            throw new SubscriptionMessageTypeException(message);
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

                if (_options.getStrategy() == SubscriptionOpeningStrategy.WAIT_FOR_FREE) {
                    if (connectionStatus.getData() != null) {
                        JsonNode registerConnectionDurationInTicksObject = connectionStatus.getData().get("RegisterConnectionDurationInTicks");
                        if (registerConnectionDurationInTicksObject.isLong()) {
                            long registerConnectionDurationInTicks = registerConnectionDurationInTicksObject.asLong();

                            if (registerConnectionDurationInTicks / 10_000 >= _options.getMaxErroneousPeriod().toMillis()) {
                                // this worker connection Waited For Free for more than MaxErroneousPeriod
                                _lastConnectionFailure = null;
                            }
                        }
                    }
                }

                ObjectNode data = connectionStatus.getData();
                JsonNode redirectedTag = data.get("RedirectedTag");
                String appropriateNode = redirectedTag.isNull() ? null : redirectedTag.asText();
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

                EventHelper.invoke(onEstablishedSubscriptionConnection, null);

                processSubscriptionInternal(tcpClientCopy);
            }

        } catch (OperationCancelledException e) {
            if (!_disposed) {
                throw e;
            }

            // otherwise this is thrown when shutting down,
            // it isn't an error, so we don't need to treat it as such
        }
    }

    private void processSubscriptionInternal(Socket tcpClientCopy) throws Exception {
        CompletableFuture<Void> notifiedSubscriber = CompletableFuture.completedFuture(null);

        try {
            TBatch batch = createEmptyBatch();

            while (!_processingCts.getToken().isCancellationRequested()) {
                prepareBatch(tcpClientCopy, batch, notifiedSubscriber);

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
                            sendAck(batch, tcpClientCopy);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, _executorService);
            }
        } finally {
            try {
                if (!notifiedSubscriber.isDone()) {
                    notifiedSubscriber.get(15, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                // ignored
            }
        }
    }

    private BatchFromServer prepareBatch(Socket tcpClientCopy, TBatch batch, CompletableFuture<Void> notifiedSubscriber) throws Exception {
        // start reading next batch from server on 1'st thread (can be before client started processing)
        CompletableFuture<BatchFromServer> readFromServer =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return readSingleSubscriptionBatchFromServer(tcpClientCopy, batch);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, _executorService);

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
        batch.initialize(incomingBatch);

        return incomingBatch;
    }

    private BatchFromServer readSingleSubscriptionBatchFromServer(Socket socket, TBatch batch) throws IOException {
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

    private void sendAck(TBatch batch, Socket networkStream) throws IOException {
        SubscriptionConnectionClientMessage msg = new SubscriptionConnectionClientMessage();
        msg.setChangeVector(batch.lastSentChangeVectorInBatch);
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

                        Tuple<Boolean, ServerNode> reconnectAndServerNode = checkIfShouldReconnectWorker(ex);
                        _redirectNode = reconnectAndServerNode.second;

                        if (reconnectAndServerNode.first) {
                            Thread.sleep(_options.getTimeToWaitBeforeConnectionRetry().toMillis());

                            if (_redirectNode == null) {
                                RequestExecutor reqEx = getRequestExecutor();
                                List<ServerNode> curTopology = reqEx.getTopologyNodes();
                                int nextNodeIndex = (_forcedTopologyUpdateAttempts++) % curTopology.size();
                                try {
                                    _redirectNode = reqEx.getRequestedNode(curTopology.get(nextNodeIndex).getClusterTag(), true).currentNode;
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
        }, _executorService);
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

    private Tuple<Boolean, ServerNode> checkIfShouldReconnectWorker(Exception ex) {
        ex = ExceptionsUtils.unwrapException(ex);
        if (ex instanceof SubscriptionDoesNotBelongToNodeException) {
            SubscriptionDoesNotBelongToNodeException se = (SubscriptionDoesNotBelongToNodeException) ex;
            RequestExecutor requestExecutor = getRequestExecutor();

            if (se.getAppropriateNode() == null) {
                assertLastConnectionFailure();

                return Tuple.create(true, null);
            }

            ServerNode nodeToRedirectTo = requestExecutor.getTopologyNodes()
                    .stream()
                    .filter(x -> x.getClusterTag().equals(se.getAppropriateNode()))
                    .findFirst()
                    .orElse(null);

            if (nodeToRedirectTo == null) {
                throw new IllegalStateException("Could not redirect to " + se.getAppropriateNode() + ", because it was not found in local topology, even after retrying");
            }

            return Tuple.create(true, nodeToRedirectTo);
        } else if (ex instanceof DatabaseDisabledException || ex instanceof AllTopologyNodesDownException) {
            assertLastConnectionFailure();
            return Tuple.create(true, _redirectNode);
        } else if (ex instanceof NodeIsPassiveException) {
            // if we failed to talk to a node, we'll forget about it and let the topology to
            // redirect us to the current node
            return Tuple.create(true, null);
        } else if (ex instanceof SubscriptionChangeVectorUpdateConcurrencyException) {
            return Tuple.create(true, _redirectNode);
        } else if (ex instanceof SubscriptionClosedException) {
            SubscriptionClosedException sce = (SubscriptionClosedException) ex;
            if (sce.isCanReconnect()) {
                return Tuple.create(true, _redirectNode);
            }

            _processingCts.cancel();
            return Tuple.create(false, _redirectNode);
        }

        if (ex instanceof SubscriptionInUseException
                || ex instanceof SubscriptionDoesNotExistException
                || ex instanceof SubscriptionInvalidStateException
                || ex instanceof DatabaseDoesNotExistException
                || ex instanceof AuthorizationException
                || ex instanceof SubscriberErrorException) {
            _processingCts.cancel();
            return Tuple.create(false, _redirectNode);
        }

        EventHelper.invoke(onUnexpectedSubscriptionError, ex);
        assertLastConnectionFailure();
        return Tuple.create(true, _redirectNode);
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

    Consumer<AbstractSubscriptionWorker<TBatch, TType>> onClosed = null;

    protected abstract RequestExecutor getRequestExecutor();

    protected abstract void setLocalRequestExecutor(String url, KeyStore cert, char[] password, KeyStore truststore);

    protected abstract TBatch createEmptyBatch();

    protected abstract void trySetRedirectNodeOnConnectToServer();

}
