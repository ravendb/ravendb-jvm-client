package net.ravendb.client.documents.changes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.exceptions.changes.ChangeProcessingException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.extensions.StringExtensions;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.UpdateTopologyParameters;
import net.ravendb.client.primitives.*;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class AbstractDatabaseChanges<TDatabaseConnectionState extends AbstractDatabaseConnectionState> implements CleanCloseable {

    private int _commandId;

    private final Semaphore _semaphore = new Semaphore(1);

    private final ExecutorService _executorService;
    protected final RequestExecutor _requestExecutor;
    protected final DocumentConventions _conventions;
    private final String _database;

    private final Runnable _onDispose;

    private final WebSocketClient _client;
    private Session _clientSession;
    private DatabaseChanges.WebSocketChangesProcessor _processor;

    private final CompletableFuture<Void> _task;
    private final CancellationTokenSource _cts;
    private CompletableFuture<AbstractDatabaseChanges<TDatabaseConnectionState>> _tcs;

    protected final ConcurrentMap<Integer, CompletableFuture<Void>> _confirmations = new ConcurrentHashMap<>();

    protected final ConcurrentMap<DatabaseChangesOptions, DatabaseConnectionState> _states = new ConcurrentHashMap<>();

    private final AtomicInteger _immediateConnection = new AtomicInteger();

    protected final CompletableFuture<ChangesSupportedFeatures> _supportedFeaturesTcs = new CompletableFuture<>();

    public CompletableFuture<ChangesSupportedFeatures> getSupportedFeatures() {
        return _supportedFeaturesTcs;
    }

    private ServerNode _serverNode;
    private int _nodeIndex;
    private String _url;

    protected AbstractDatabaseChanges(RequestExecutor requestExecutor, String databaseName, ExecutorService executorService, Runnable onDispose, String nodeTag) {
        _executorService = executorService;
        _requestExecutor = requestExecutor;
        _conventions = requestExecutor.getConventions();
        _database = databaseName;

        _tcs = new CompletableFuture<>();
        _cts = new CancellationTokenSource();

        _client = createWebSocketClient(_requestExecutor);
        _supportedFeaturesTcs.thenAcceptAsync(t -> {
            if (!t.isTopologyChange()) {
                return;
            }
            getOrAddConnectionState("Topology", "watch-topology-change", "", "");
            UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(_serverNode);
            updateParameters.setTimeoutInMs(0);
            updateParameters.setForceUpdate(true);
            updateParameters.setDebugTag("watch-topology-change");
            _requestExecutor.updateTopologyAsync(updateParameters);
        }, _executorService);

        _onDispose = onDispose;
        addConnectionStatusChanged(_connectionStatusEventHandler);

        _task = CompletableFuture.runAsync(() -> doWork(nodeTag), executorService);
    }

    protected WebSocketClient createWebSocketClient(RequestExecutor requestExecutor) {
        WebSocketClient client;

        try {
            if (requestExecutor.getCertificate() != null) {
                SSLContext sslContext = requestExecutor.createSSLContext();
                SslContextFactory factory = new SslContextFactory.Client();
                factory.setSslContext(sslContext);

                HttpClient httpClient = new HttpClient(factory);
                client = new WebSocketClient(httpClient);
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

    public AbstractDatabaseChanges<TDatabaseConnectionState> ensureConnectedNow() {
        try {
            return _tcs.get();
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

    private final List<Consumer<Exception>> onError = new ArrayList<>();

    public void addOnError(Consumer<Exception> handler) {
        this.onError.add(handler);
    }

    public void removeOnError(Consumer<Exception> handler) {
        this.onError.remove(handler);
    }


    @Override
    public void close() {
        try {
            for (CompletableFuture<Void> confirmation : _confirmations.values()) {
                confirmation.cancel(false);
            }

            _cts.cancel();

            if (_clientSession != null) {
                IOUtils.closeQuietly(_clientSession, null);
            }

            if (_client != null) {
                _client.stop();
            }

            if (_clientSession != null) {
                IOUtils.closeQuietly(_clientSession, null);
            }

            for (DatabaseConnectionState value : _states.values()) {
                value.close();
            }

            _states.clear();

            try {
                _task.get();
            } catch (Exception e) {
                //we're disposing the document store
                // nothing we can do here
            }

            try {
                EventHelper.invoke(_connectionStatusChanged, this, EventArgs.EMPTY);
            } catch (Exception e) {
                // we are disposing
            }
            removeConnectionStatusChanged(_connectionStatusEventHandler);

            if (_onDispose != null) {
                _onDispose.run();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to close DatabaseChanges" + e.getMessage(), e);
        }
    }

    protected DatabaseConnectionState getOrAddConnectionState(String name, String watchCommand, String unwatchCommand, String value) {
        return getOrAddConnectionState(name, watchCommand, unwatchCommand, value, null);
    }

    protected DatabaseConnectionState getOrAddConnectionState(String name, String watchCommand, String unwatchCommand, String value, String[] values) {
        Reference<Boolean> newValue = new Reference<>();

        DatabaseConnectionState counter = _states.computeIfAbsent(new DatabaseChangesOptions(name, null), s -> {

            Runnable onDisconnect = () -> {
                try {
                    if (isConnected()) {
                        send(unwatchCommand, value, values);
                    }
                } catch (Exception e) {
                    // if we are not connected then we unsubscribed already
                    // because connections drops with all subscriptions
                }

                DatabaseConnectionState state = _states.get(s);
                _states.remove(s);
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

    private void doWork(String nodeTag) {
        CurrentIndexAndNode preferredNode;
        try {
            preferredNode = nodeTag == null || _requestExecutor.getConventions().isDisableTopologyUpdates() ?
                    _requestExecutor.getPreferredNode() :
                    _requestExecutor.getRequestedNode(nodeTag);
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

                    _processor = new AbstractDatabaseChanges.WebSocketChangesProcessor();
                    ClientUpgradeRequest request = new ClientUpgradeRequest();
                    request.setTimeout(10_000, TimeUnit.MILLISECONDS);
                    _clientSession = _client.connect(_processor, url, request).get();
                    wasConnected = true;

                    _immediateConnection.set(1);

                    for (DatabaseConnectionState counter : _states.values()) {
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
                    } catch (DatabaseDoesNotExistException databaseDoesNotExistException) {
                        e = databaseDoesNotExistException;
                        throw databaseDoesNotExistException;
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
                    _tcs.completeExceptionally(ee);
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

    protected abstract void processNotification(String type, ObjectNode change) throws JsonProcessingException;

    private void processConfirmationNotification(ObjectNode json) {
        int commandId = json.get("CommandId").asInt();
        CompletableFuture<Void> future = _confirmations.remove(commandId);
        if (future != null) {
            future.complete(null);
        }
    }

    private void processErrorNotification(ObjectNode json) {
        String exceptionAsString = json.get("Exception").asText();
        notifyAboutError(new RuntimeException(exceptionAsString));
    }

    protected void notifyAboutError(Exception e) {
        if (_cts.getToken().isCancellationRequested()) {
            return;
        }

        EventHelper.invoke(onError, e);

        for (DatabaseConnectionState state : _states.values()) {
            state.error(e);
        }
    }

    protected abstract void notifySubscribers(String type, ObjectNode value) throws JsonProcessingException;

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
                            ChangesSupportedFeatures supportedFeatures = JsonExtensions.getDefaultMapper().treeToValue(msgNode, ChangesSupportedFeatures.class);
                            _supportedFeaturesTcs.complete(supportedFeatures);
                            continue;
                        }

                        JsonNode typeAsJson = msgNode.get("Type");
                        if (typeAsJson == null) {
                            continue;
                        }

                        String type = typeAsJson.asText();
                        switch (type) {
                            case "Error":
                                processErrorNotification(msgNode);
                                break;
                            case "Confirm":
                                processConfirmationNotification(msgNode);
                                break;
                            default:
                                processNotification(type, msgNode);
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
}
