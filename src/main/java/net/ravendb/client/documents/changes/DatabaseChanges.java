package net.ravendb.client.documents.changes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.exceptions.changes.ChangeProcessingException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.extensions.StringExtensions;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContexts;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SuppressWarnings("UnnecessaryLocalVariable")
public class DatabaseChanges implements IDatabaseChanges {

    private int _commandId;

    private final Semaphore _semaphore = new Semaphore(1);

    private final ExecutorService _executorService;
    private final RequestExecutor _requestExecutor;
    private final DocumentConventions _conventions;
    private final String _database;

    private final Runnable _onDispose;

    private final WebSocketClient _client;
    private Session _clientSession;
    private WebSocketChangesProcessor _processor;

    private final CompletableFuture<Void> _task;
    private final CancellationTokenSource _cts;
    private CompletableFuture<IDatabaseChanges> _tcs;

    private final ConcurrentMap<Integer, CompletableFuture<Void>> _confirmations = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, DatabaseConnectionState> _counters = new ConcurrentSkipListMap<>(String::compareToIgnoreCase);

    private final AtomicInteger _immediateConnection = new AtomicInteger();

    private ServerNode _serverNode;
    private int _nodeIndex;
    private String _url;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public DatabaseChanges(RequestExecutor requestExecutor, String databaseName, ExecutorService executorService, Runnable onDispose) {
        _executorService = executorService;
        _requestExecutor = requestExecutor;
        _conventions = requestExecutor.getConventions();
        _database = databaseName;

        _tcs = new CompletableFuture<>();
        _cts = new CancellationTokenSource();

        _client = createWebSocketClient(_requestExecutor);
        _onDispose = onDispose;
        addConnectionStatusChanged(_connectionStatusEventHandler);

        _task = CompletableFuture.runAsync(() -> doWork(), executorService);
    }

    public static WebSocketClient createWebSocketClient(RequestExecutor requestExecutor) {
        WebSocketClient client;

        try {
            if (requestExecutor.getCertificate() != null) {
                SSLContext sslContext = requestExecutor.createSSLContext();
                SslContextFactory factory = new SslContextFactory();
                factory.setSslContext(sslContext);
                client = new WebSocketClient(factory);
            } else {
                client = new WebSocketClient();
            }

            client.start();
        } catch (Exception e) {
            throw ExceptionsUtils.unwrapException(e);
        }

        return client;

    }

    private void onConnectionStatusChanged(Object sender, EventArgs eventArgs) {
        try {
            _semaphore.acquire();


            if (isConnected()) {
                _tcs.complete(this);
                return;
            }

            if (_tcs.isDone()) {
                _tcs = new CompletableFuture<>();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            _semaphore.release();
        }
    }
    public boolean isConnected() {
        return _clientSession != null && _clientSession.isOpen();
    }

    @Override
    public void ensureConnectedNow() {
        try {
            _tcs.get();
        } catch (Exception e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }
    private final List<EventHandler<VoidArgs>> _connectionStatusChanged = new ArrayList<>();

    private final EventHandler<VoidArgs> _connectionStatusEventHandler = (sender, event) -> onConnectionStatusChanged(sender, event);
    public void addConnectionStatusChanged(EventHandler<VoidArgs> handler) {
        _connectionStatusChanged.add(handler);
    }

    public void removeConnectionStatusChanged(EventHandler<VoidArgs> handler) {
        _connectionStatusChanged.remove(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IChangesObservable<IndexChange> forIndex(String indexName) {
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException("IndexName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("indexes/" + indexName, "watch-index", "unwatch-index", indexName);

        ChangesObservable taskedObservable = new ChangesObservable<IndexChange, DatabaseConnectionState>(
                ChangesType.INDEX, counter, notification -> StringUtils.equalsIgnoreCase(notification.getName(), indexName));

        return taskedObservable;
    }

    public Exception getLastConnectionStateException() {
        for (DatabaseConnectionState counter : _counters.values()) {

            Exception valueLastException = counter.lastException;
            if (valueLastException != null) {
                return valueLastException;
            }
        }

        return null;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocument(String docId) {
        if (StringUtils.isBlank(docId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace");
        }
        DatabaseConnectionState counter = getOrAddConnectionState("docs/" + docId, "watch-doc", "unwatch-doc", docId);

        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> StringUtils.equalsIgnoreCase(notification.getId(), docId));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forAllDocuments() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-docs", "watch-docs", "unwatch-docs", null);
        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<OperationStatusChange> forOperationId(long operationId) {
        DatabaseConnectionState counter = getOrAddConnectionState("operations/" + operationId, "watch-operation", "unwatch-operation", String.valueOf(operationId));

        ChangesObservable<OperationStatusChange, DatabaseConnectionState> taskedObservable
                = new ChangesObservable<>(ChangesType.OPERATION, counter, notification -> notification.getOperationId() == operationId);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<OperationStatusChange> forAllOperations() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-operations", "watch-operations", "unwatch-operations", null);

        ChangesObservable<OperationStatusChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.OPERATION, counter,
                notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<IndexChange> forAllIndexes() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-indexes", "watch-indexes", "unwatch-indexes", null);

        ChangesObservable<IndexChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.INDEX, counter, notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsStartingWith(String docIdPrefix) {
        if (StringUtils.isBlank(docIdPrefix)) {
            throw new IllegalArgumentException("DocumentIdPrefix cannot be null or whitespace");
        }
        DatabaseConnectionState counter = getOrAddConnectionState("prefixes/" + docIdPrefix, "watch-prefix", "unwatch-prefix", docIdPrefix);
        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> notification.getId() != null && StringUtils.startsWithIgnoreCase(notification.getId(), docIdPrefix));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsInCollection(String collectionName) {
        if (StringUtils.isBlank(collectionName)) {
            throw new IllegalArgumentException("CollectionName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("collections/" + collectionName, "watch-collection", "unwatch-collection", collectionName);

        ChangesObservable<DocumentChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.DOCUMENT, counter,
                notification -> StringUtils.equalsIgnoreCase(collectionName, notification.getCollectionName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<DocumentChange> forDocumentsInCollection(Class<?> clazz) {
        String collectionName = _conventions.getCollectionName(clazz);
        return forDocumentsInCollection(collectionName);
    }

    @Override
    public IChangesObservable<CounterChange> forAllCounters() {
        DatabaseConnectionState counter = getOrAddConnectionState("all-counters", "watch-counters", "unwatch-counters", null);

        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> true);

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCounter(String counterName) {
        if (StringUtils.isBlank(counterName)) {
            throw new IllegalArgumentException("CounterName cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("counter/" + counterName, "watch-counter", "unwatch-counter", counterName);
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(counterName, notification.getName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCounterOfDocument(String documentId, String counterName) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace.");
        }
        if (StringUtils.isBlank(counterName)) {
            throw new IllegalArgumentException("CounterName cannot be null or whitespace.");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/counter/" + counterName, "watch-document-counter", "unwatch-document-counter", null, new String[]{documentId, counterName});
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId()) && StringUtils.equalsIgnoreCase(counterName, notification.getName()));

        return taskedObservable;
    }

    @Override
    public IChangesObservable<CounterChange> forCountersOfDocument(String documentId) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or whitespace");
        }

        DatabaseConnectionState counter = getOrAddConnectionState("document/" + documentId + "/counter", "watch-document-counters", "unwatch-document-counters", documentId);
        ChangesObservable<CounterChange, DatabaseConnectionState> taskedObservable = new ChangesObservable<>(ChangesType.COUNTER, counter,
                notification -> StringUtils.equalsIgnoreCase(documentId, notification.getDocumentId()));

        return taskedObservable;
    }

    private final List<Consumer<Exception>> onError = new ArrayList<>();

    @Override
    public void addOnError(Consumer<Exception> handler) {
        this.onError.add(handler);
    }

    @Override
    public void removeOnError(Consumer<Exception> handler) {
        this.onError.remove(handler);
    }

    @Override
    public void close() {
        try {
            for (CompletableFuture<Void> confirmation : _confirmations.values()) {
                confirmation.cancel(false);
            }

            _semaphore.acquire();
            try {
                if (_clientSession != null) {
                    _clientSession.close();
                }
            } finally {
                _semaphore.release();
            }

            _cts.cancel();

            if (_client != null) {
                _client.stop();
            }

            _counters.clear();

            try {
                _task.get();
            } catch (Exception e) {
                //we're disposing the document store
                // nothing we can do here
            }

            EventHelper.invoke(_connectionStatusChanged, this, EventArgs.EMPTY);
            removeConnectionStatusChanged(_connectionStatusEventHandler);

            _onDispose.run();
        } catch (Exception e) {
            throw new RuntimeException("Unable to close DatabaseChanges" + e.getMessage(), e);
        }
    }

    private DatabaseConnectionState getOrAddConnectionState(String name, String watchCommand, String unwatchCommand, String value) {
        return getOrAddConnectionState(name, watchCommand, unwatchCommand, value, null);
    }

    private DatabaseConnectionState getOrAddConnectionState(String name, String watchCommand, String unwatchCommand, String value, String[] values) {
        Reference<Boolean> newValue = new Reference<>();

        DatabaseConnectionState counter = _counters.computeIfAbsent(name, s -> {

            Runnable onDisconnect = () -> {
                try {
                    if (isConnected()) {
                        send(unwatchCommand, value, values);
                    }
                } catch (Exception e) {
                    // if we are not connected then we unsubscribed already
                    // because connections drops with all subscriptions
                }

                DatabaseConnectionState state = _counters.get(s);
                _counters.remove(s);
                state.close();
            };

            Runnable onConnect = () -> send(watchCommand, value, values);

            newValue.value = true;
            return new DatabaseConnectionState(onConnect, onDisconnect);
        });

        if (newValue.value && _immediateConnection.get() != 0) {
            counter.onConnect.run();
        }

        return counter;
    }

    private void send(String command, String value, String[] values) {
        CompletableFuture<Void> taskCompletionSource = new CompletableFuture<>();
        int currentCommandId;

        try {
            _semaphore.acquire();

            currentCommandId = ++_commandId;
            StringWriter writer = new StringWriter();
            try (JsonGenerator generator = JsonExtensions.getDefaultMapper().getFactory().createGenerator(writer)) {
                generator.writeStartObject();

                generator.writeNumberField("CommandId", currentCommandId);
                generator.writeStringField("Command", command);
                generator.writeStringField("Param", value);

                if (values != null && values.length > 0) {
                    generator.writeFieldName("Params");
                    generator.writeStartArray();
                    for (String param : values) {
                        generator.writeString(param);
                    }
                    generator.writeEndArray();
                }

                generator.writeEndObject();
            }

            _confirmations.put(currentCommandId, taskCompletionSource);

            if (!_clientSession.isOpen()) {
                throw new RuntimeException("Unable to send command: " + command + ". Session is closed.");
            }
            _clientSession.getRemote().sendString(writer.toString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Unable to send command: " + command, e);
        } finally {
            _semaphore.release();
        }

        try {
            taskCompletionSource.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new TimeoutException("Did not get a confirmation for command #" + currentCommandId, e);
        }
    }

    private void doWork() {
        CurrentIndexAndNode preferredNode;
        try {
            preferredNode = _requestExecutor.getPreferredNode();
            _nodeIndex = preferredNode.currentIndex;
            _serverNode = preferredNode.currentNode;
        } catch (Exception e) {
            EventHelper.invoke(_connectionStatusChanged, this, EventArgs.EMPTY);
            notifyAboutError(e);
            _tcs.completeExceptionally(e);
            return;
        }

        boolean wasConnected = false;
        while (!_cts.getToken().isCancellationRequested()) {
            try {
                if (!isConnected()) {
                    String urlString = _serverNode.getUrl() + "/databases/" + _database + "/changes";
                    URI url;
                    try {
                        url = new URI(StringExtensions.toWebSocketPath(urlString.toLowerCase()));
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }

                    _processor = new WebSocketChangesProcessor();
                    ClientUpgradeRequest request = new ClientUpgradeRequest();
                    _clientSession = _client.connect(_processor, url, request).get();
                    wasConnected = true;

                    _immediateConnection.set(1);

                    for (DatabaseConnectionState counter : _counters.values()) {
                        counter.onConnect.run();
                    }

                    EventHelper.invoke(_connectionStatusChanged, this, EventArgs.EMPTY);
                }

                _processor.processing.get();
            } catch (Exception e) {

                if (e instanceof ExecutionException && e.getCause() instanceof ChangeProcessingException) {
                    continue;
                }

                try {
                    if (wasConnected) {
                        EventHelper.invoke(_connectionStatusChanged, this, EventArgs.EMPTY);
                    }
                    wasConnected = false;

                    try {
                        _serverNode = _requestExecutor.handleServerNotResponsive(_url, _serverNode, _nodeIndex, e);
                    } catch (Exception ee) {
                        //We don't want to stop observe for changes if server down. we will wait for one to be up
                    }

                    if (!reconnectClient()) {
                        return;
                    }
                } catch (Exception ee) {
                    // we couldn't reconnect
                    RuntimeException unwrappedException = ExceptionsUtils.unwrapException(e);
                    notifyAboutError(unwrappedException);
                    throw unwrappedException;
                }

            } finally {
                for (CompletableFuture<Void> confirmation : _confirmations.values()) {
                    confirmation.cancel(false);
                }

                _confirmations.clear();
            }

            try {
                // wait before next retry
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

    }

    private boolean reconnectClient() {
        if (_cts.getToken().isCancellationRequested()) {
            return false;
        }

        _immediateConnection.set(0);
        return true;
    }

    @WebSocket
    public class WebSocketChangesProcessor {
        public final CompletableFuture<Void> processing = new CompletableFuture<>();

        @OnWebSocketError
        public void onError(Session session, Throwable error) {
            processing.completeExceptionally(error);
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            try {
                JsonNode messages = JsonExtensions.getDefaultMapper().readTree(msg);
                if (messages instanceof ArrayNode) {
                    ArrayNode msgArray = (ArrayNode) messages;

                    for (int i = 0; i < msgArray.size(); i++) {
                        ObjectNode msgNode = (ObjectNode) msgArray.get(i);

                        JsonNode topologyChange = msgNode.get("TopologyChange");
                        if (topologyChange != null && topologyChange.isBoolean() && topologyChange.asBoolean()) {
                            // process this in async way, as getOrAddConnectionStats waits for confirmation, so we can't block this thread
                            CompletableFuture.runAsync(() -> {
                                getOrAddConnectionState("Topology", "watch-topology-change", "", "");
                                _requestExecutor.updateTopologyAsync(_serverNode, 0, true, "watch-topology-change");
                            }, _executorService);
                            continue;
                        }

                        JsonNode typeAsJson = msgNode.get("Type");
                        if (typeAsJson == null) {
                            continue;
                        }

                        String type = typeAsJson.asText();
                        switch (type) {
                            case "Error":
                                String exceptionAsString = msgNode.get("Exception").asText();
                                notifyAboutError(new RuntimeException(exceptionAsString));
                                break;
                            case "Confirm":
                                int commandId = msgNode.get("CommandId").asInt();
                                CompletableFuture<Void> future = _confirmations.remove(commandId);
                                if (future != null) {
                                    future.complete(null);
                                }
                                break;
                            default:
                                ObjectNode value = (ObjectNode) msgNode.get("Value");
                                notifySubscribers(type, value, _counters.values());
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                notifyAboutError(e);
                throw new ChangeProcessingException(e);
            }
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            // it might be normal shutdown, but we throw
            // cancellation token is checked in catch block
            processing.completeExceptionally(new RuntimeException("WebSocket closed"));
        }
    }

    private void notifySubscribers(String type, ObjectNode value, Collection<DatabaseConnectionState> states) throws JsonProcessingException {
        switch (type) {
            case "DocumentChange":
                DocumentChange documentChange = JsonExtensions.getDefaultMapper().treeToValue(value, DocumentChange.class);
                for (DatabaseConnectionState state : states) {
                    state.send(documentChange);
                }
                break;
            case "CounterChange":
                CounterChange counterChange = JsonExtensions.getDefaultMapper().treeToValue(value, CounterChange.class);
                for (DatabaseConnectionState state : states) {
                    state.send(counterChange);
                }
                break;
            case "IndexChange":
                IndexChange indexChange = JsonExtensions.getDefaultMapper().treeToValue(value, IndexChange.class);
                for (DatabaseConnectionState state : states) {
                    state.send(indexChange);
                }
                break;
            case "OperationStatusChange":
                OperationStatusChange operationStatusChange = JsonExtensions.getDefaultMapper().treeToValue(value, OperationStatusChange.class);
                for (DatabaseConnectionState state : states) {
                    state.send(operationStatusChange);
                }
                break;

            case "TopologyChange":
                TopologyChange topologyChange = JsonExtensions.getDefaultMapper().treeToValue(value, TopologyChange.class);
                if (_requestExecutor != null) {
                    ServerNode serverNode = new ServerNode();
                    serverNode.setUrl(topologyChange.getUrl());
                    serverNode.setDatabase(topologyChange.getDatabase());
                    _requestExecutor.updateTopologyAsync(serverNode, 0, true, "topology-change-notification");
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void notifyAboutError(Exception e) {
        if (_cts.getToken().isCancellationRequested()) {
            return;
        }

        EventHelper.invoke(onError, e);

        for (DatabaseConnectionState state : _counters.values()) {
            state.error(e);
        }
    }

}
