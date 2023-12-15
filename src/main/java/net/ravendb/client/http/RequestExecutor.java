package net.ravendb.client.http;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.DatabaseHealthCheckOperation;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.configuration.GetClientConfigurationOperation;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.exceptions.*;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.exceptions.security.AuthorizationException;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.primitives.*;
import net.ravendb.client.primitives.Timer;
import net.ravendb.client.serverwide.commands.GetDatabaseTopologyCommand;
import net.ravendb.client.serverwide.commands.GetNodeInfoCommand;
import net.ravendb.client.util.CertificateUtils;
import net.ravendb.client.util.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class RequestExecutor implements CleanCloseable {

    private static UUID GLOBAL_APPLICATION_IDENTIFIER = UUID.randomUUID();

    private static final int INITIAL_TOPOLOGY_ETAG = -2;

    public static Consumer<HttpClientBuilder> configureHttpClient = null;

    private static final GetStatisticsOperation backwardCompatibilityFailureCheckOperation = new GetStatisticsOperation("failure=check");

    private static final DatabaseHealthCheckOperation failureCheckOperation = new DatabaseHealthCheckOperation();
    private static Set<String> _useOldFailureCheckOperation = ConcurrentHashMap.newKeySet();

    /**
     * Extension point to plug - in request post processing like adding proxy etc.
     */
    public static Consumer<HttpRequestBase> requestPostProcessor = null;

    public static final String CLIENT_VERSION = "6.0.0";

    private static final ConcurrentMap<String, CloseableHttpClient> globalHttpClientWithCompression = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, CloseableHttpClient> globalHttpClientWithoutCompression = new ConcurrentHashMap<>();

    private final Semaphore _updateDatabaseTopologySemaphore = new Semaphore(1);

    private final Semaphore _updateClientConfigurationSemaphore = new Semaphore(1);

    private final ConcurrentMap<ServerNode, NodeStatus> _failedNodesTimers = new ConcurrentHashMap<>();

    private final KeyStore certificate;
    private final char[] keyPassword;
    private final KeyStore trustStore;

    private final String _databaseName;

    private static final Log logger = LogFactory.getLog(RequestExecutor.class);
    private Date _lastReturnedResponse;

    protected final ExecutorService _executorService;

    private final HttpCache cache;

    private ServerNode _topologyTakenFromNode;

    public HttpCache getCache() {
        return cache;
    }

    public final ThreadLocal<AggressiveCacheOptions> aggressiveCaching = new ThreadLocal<>();

    public Topology getTopology() {
        return _nodeSelector != null ? _nodeSelector.getTopology() : null;
    }

    private CloseableHttpClient _httpClient;

    public CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = _httpClient;
        if (httpClient != null) {
            return httpClient;
        }

        return _httpClient = createHttpClient();
    }

    public List<ServerNode> getTopologyNodes() {
        return Optional.ofNullable(getTopology())
                .map(Topology::getNodes)
                .map(Collections::unmodifiableList)
                .orElse(null);
    }

    private volatile Timer _updateTopologyTimer;

    protected NodeSelector _nodeSelector;

    private Duration _defaultTimeout;

    public final AtomicLong numberOfServerRequests = new AtomicLong(0);

    public String getUrl() {
        if (_nodeSelector == null) {
            return null;
        }

        CurrentIndexAndNode preferredNode = _nodeSelector.getPreferredNode();

        return preferredNode != null ? preferredNode.currentNode.getUrl() : null;
    }

    protected long topologyEtag;

    public long getTopologyEtag() {
        return topologyEtag;
    }

    protected long clientConfigurationEtag;

    public long getClientConfigurationEtag() {
        return clientConfigurationEtag;
    }

    private final DocumentConventions conventions;

    protected boolean _disableTopologyUpdates;

    protected boolean _disableClientConfigurationUpdates;

    protected String _topologyHeaderName = Constants.Headers.TOPOLOGY_ETAG;

    protected String lastServerVersion;

    public String getLastServerVersion() {
        return lastServerVersion;
    }

    public Duration getDefaultTimeout() {
        return _defaultTimeout;
    }

    public void setDefaultTimeout(Duration timeout) {
        _defaultTimeout = timeout;
    }

    private Duration _secondBroadcastAttemptTimeout;

    public Duration getSecondBroadcastAttemptTimeout() {
        return _secondBroadcastAttemptTimeout;
    }

    public void setSecondBroadcastAttemptTimeout(Duration secondBroadcastAttemptTimeout) {
        _secondBroadcastAttemptTimeout = secondBroadcastAttemptTimeout;
    }

    private Duration _firstBroadcastAttemptTimeout;

    public Duration getFirstBroadcastAttemptTimeout() {
        return _firstBroadcastAttemptTimeout;
    }

    public void setFirstBroadcastAttemptTimeout(Duration firstBroadcastAttemptTimeout) {
        _firstBroadcastAttemptTimeout = firstBroadcastAttemptTimeout;
    }

    private final List<EventHandler<FailedRequestEventArgs>> _onFailedRequest = new ArrayList<>();

    public void addOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler) {
        this._onFailedRequest.add(handler);
    }

    public void removeOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler) {
        this._onFailedRequest.remove(handler);
    }

    private final List<EventHandler<BeforeRequestEventArgs>> _onBeforeRequest = new ArrayList<>();

    public void addOnBeforeRequestListener(EventHandler<BeforeRequestEventArgs> handler) {
        this._onBeforeRequest.add(handler);
    }

    public void removeOnBeforeRequestListener(EventHandler<BeforeRequestEventArgs> handler) {
        this._onBeforeRequest.remove(handler);
    }

    private final List<EventHandler<SucceedRequestEventArgs>> _onSucceedRequest = new ArrayList<>();

    public void addOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler) {
        this._onSucceedRequest.add(handler);
    }

    public void removeOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler) {
        this._onSucceedRequest.remove(handler);
    }

    private final List<EventHandler<TopologyUpdatedEventArgs>> _onTopologyUpdated = new ArrayList<>();

    public void addOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler) {
        _onTopologyUpdated.add(handler);
    }

    public void removeOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler) {
        _onTopologyUpdated.remove(handler);
    }

    private void onFailedRequestInvoke(String url, Exception e) {
        onFailedRequestInvoke(url, e, null, null);
    }

    private void onFailedRequestInvoke(String url, Exception e, HttpRequest request, HttpResponse response) {
        EventHelper.invoke(_onFailedRequest, this, new FailedRequestEventArgs(_databaseName, url, e, request, response));
    }

    private CloseableHttpClient createHttpClient() {
        ConcurrentMap<String, CloseableHttpClient> httpClientCache = getHttpClientCache();

        String name = getHttpClientName();

        return httpClientCache.computeIfAbsent(name, n -> createClient());
    }

    private String getHttpClientName() {
        if (certificate != null) {
            return CertificateUtils.extractThumbprintFromCertificate(certificate);
        }
        return "";
    }

    private ConcurrentMap<String, CloseableHttpClient> getHttpClientCache() {
        return conventions.isUseCompression() ? globalHttpClientWithCompression : globalHttpClientWithoutCompression;
    }

    public DocumentConventions getConventions() {
        return conventions;
    }

    public KeyStore getCertificate() {
        return certificate;
    }

    public char[] getKeyPassword() {
        return keyPassword;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    protected RequestExecutor(String databaseName, KeyStore certificate, char[] keyPassword, KeyStore trustStore, DocumentConventions conventions, ExecutorService executorService, String[] initialUrls) {
        cache = new HttpCache(conventions.getMaxHttpCacheSize());
        _executorService = executorService;
        _databaseName = databaseName;
        this.certificate = certificate;
        this.keyPassword = keyPassword;
        this.trustStore = trustStore;

        _lastReturnedResponse = new Date();
        this.conventions = conventions.clone();
        this._defaultTimeout = conventions.getRequestTimeout();
        this._secondBroadcastAttemptTimeout = conventions.getSecondBroadcastAttemptTimeout();
        this._firstBroadcastAttemptTimeout = conventions.getFirstBroadcastAttemptTimeout();
    }

    public static RequestExecutor create(String[] initialUrls, String databaseName, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        RequestExecutor executor = new RequestExecutor(databaseName, certificate, keyPassword, trustStore, conventions, executorService, initialUrls);
        executor._firstTopologyUpdate = executor.firstTopologyUpdate(initialUrls, GLOBAL_APPLICATION_IDENTIFIER);
        return executor;
    }

    public static RequestExecutor createForSingleNodeWithConfigurationUpdates(String url, String databaseName, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        RequestExecutor executor = createForSingleNodeWithoutConfigurationUpdates(url, databaseName, certificate, keyPassword, trustStore, executorService, conventions);
        executor._disableClientConfigurationUpdates = false;
        return executor;
    }

    public static RequestExecutor createForSingleNodeWithoutConfigurationUpdates(String url, String databaseName, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        final String[] initialUrls = validateUrls(new String[]{url}, certificate);

        RequestExecutor executor = new RequestExecutor(databaseName, certificate, keyPassword, trustStore, conventions, executorService, initialUrls);

        Topology topology = new Topology();
        topology.setEtag(-1L);

        ServerNode serverNode = new ServerNode();
        serverNode.setDatabase(databaseName);
        serverNode.setUrl(initialUrls[0]);
        serverNode.setServerRole(ServerNode.Role.MEMBER);
        topology.setNodes(Collections.singletonList(serverNode));

        executor._nodeSelector = new NodeSelector(topology, executorService);
        executor.topologyEtag = INITIAL_TOPOLOGY_ETAG;
        executor._disableTopologyUpdates = true;
        executor._disableClientConfigurationUpdates = true;
        executor._firstTopologyUpdate = executor.singleTopologyUpdateAsync(initialUrls, GLOBAL_APPLICATION_IDENTIFIER);

        return executor;
    }

    protected CompletableFuture<Void> updateClientConfigurationAsync(ServerNode serverNode) {
        if (_disposed) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                _updateClientConfigurationSemaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            boolean oldDisableClientConfigurationUpdates = _disableClientConfigurationUpdates;
            _disableClientConfigurationUpdates = true;

            try {
                if (_disposed) {
                    return;
                }

                GetClientConfigurationOperation.GetClientConfigurationCommand command = new GetClientConfigurationOperation.GetClientConfigurationCommand();
                execute(serverNode, null, command, false, null);

                GetClientConfigurationOperation.Result result = command.getResult();
                if (result == null) {
                    return;
                }

                conventions.updateFrom(result.getConfiguration());
                clientConfigurationEtag = result.getEtag();
            } finally {
                _disableClientConfigurationUpdates = oldDisableClientConfigurationUpdates;
                _updateClientConfigurationSemaphore.release();
            }
        }, _executorService);
    }

    public CompletableFuture<Boolean> updateTopologyAsync(UpdateTopologyParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (_disableTopologyUpdates) {
            return CompletableFuture.completedFuture(false);
        }

        if (_disposed) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {

            //prevent double topology updates if execution takes too much time
            // --> in cases with transient issues
            try {
                boolean lockTaken = _updateDatabaseTopologySemaphore.tryAcquire(parameters.getTimeoutInMs(), TimeUnit.MILLISECONDS);
                if (!lockTaken) {
                    return false;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {

                if (_disposed) {
                    return false;
                }

                GetDatabaseTopologyCommand command = new GetDatabaseTopologyCommand(parameters.getDebugTag(),
                        getConventions().isSendApplicationIdentifier() ? parameters.getApplicationIdentifier() : null);

                if (_defaultTimeout != null && _defaultTimeout.compareTo(command.getTimeout()) > 0) {
                    command.setTimeout(_defaultTimeout);
                }

                execute(parameters.getNode(), null, command, false, null);
                Topology topology = command.getResult();

                if (_nodeSelector == null) {
                    _nodeSelector = new NodeSelector(topology, _executorService);

                    if (conventions.getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                } else if (_nodeSelector.onUpdateTopology(topology, parameters.isForceUpdate())) {
                    disposeAllFailedNodesTimers();
                    if (conventions.getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                }

                topologyEtag = _nodeSelector.getTopology().getEtag();

                onTopologyUpdatedInvoke(topology);
            } catch (Exception e) {
                if (!_disposed) {
                    throw e;
                }
            } finally {
                _updateDatabaseTopologySemaphore.release();
            }

            return true;
        }, _executorService);

    }

    protected void updateNodeSelector(Topology topology, boolean forceUpdate) {
        if (_nodeSelector == null) {
            _nodeSelector = new NodeSelector(topology, _executorService);

            if (getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                _nodeSelector.scheduleSpeedTest();
            }
        } else if (_nodeSelector.onUpdateTopology(topology, forceUpdate)) {
            disposeAllFailedNodesTimers();

            if (getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                _nodeSelector.scheduleSpeedTest();
            }
        }

        topologyEtag = _nodeSelector.getTopology().getEtag();
    }

    protected void disposeAllFailedNodesTimers() {
        _failedNodesTimers.forEach((node, status) -> status.close());
        _failedNodesTimers.clear();
    }

    public <TResult> void execute(RavenCommand<TResult> command) {
        execute(command, null);
    }

    public <TResult> void execute(RavenCommand<TResult> command, SessionInfo sessionInfo) {
        CompletableFuture<Void> topologyUpdate = _firstTopologyUpdate;
        if (topologyUpdate != null &&
                (topologyUpdate.isDone() && !topologyUpdate.isCompletedExceptionally() && !topologyUpdate.isCancelled())) {
            CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, sessionInfo);
            execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, true, sessionInfo);
        } else {
            unlikelyExecute(command, topologyUpdate, sessionInfo);
        }
    }

    public <TResult> CurrentIndexAndNode chooseNodeForRequest(RavenCommand<TResult> cmd, SessionInfo sessionInfo) {
        if (StringUtils.isNotBlank(cmd.getSelectedNodeTag())) {
            return _nodeSelector.getRequestedNode(cmd.getSelectedNodeTag());
        }

        if (conventions.getLoadBalanceBehavior() == LoadBalanceBehavior.USE_SESSION_CONTEXT) {
            if (sessionInfo != null && sessionInfo.canUseLoadBalanceBehavior()) {
                return _nodeSelector.getNodeBySessionId(sessionInfo.getSessionId());
            }
        }

        if (!cmd.isReadRequest()) {
            return _nodeSelector.getPreferredNode();
        }

        switch (conventions.getReadBalanceBehavior()) {
            case NONE:
                return _nodeSelector.getPreferredNode();
            case ROUND_ROBIN:
                return _nodeSelector.getNodeBySessionId(sessionInfo != null ? sessionInfo.getSessionId() : 0);
            case FASTEST_NODE:
                return _nodeSelector.getFastestNode();
            default:
                throw new IllegalArgumentException();
        }
    }

    private <TResult> void unlikelyExecute(RavenCommand<TResult> command, CompletableFuture<Void> topologyUpdate, SessionInfo sessionInfo) {
        waitForTopologyUpdate(topologyUpdate);

        CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, sessionInfo);
        execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, true, sessionInfo);
    }

    private void waitForTopologyUpdate(CompletableFuture<Void> topologyUpdate) {
        try {
            if (topologyUpdate == null || topologyUpdate.isCompletedExceptionally()) {
                synchronized (this) {
                    if (_firstTopologyUpdate == null || topologyUpdate == _firstTopologyUpdate) {
                        if (_lastKnownUrls == null) {
                            // shouldn't happen
                            throw new IllegalStateException("No known topology and no previously known one, cannot proceed, likely a bug");
                        }

                        if (!_disableTopologyUpdates) {
                            _firstTopologyUpdate = firstTopologyUpdate(_lastKnownUrls, null);
                        } else {
                            _firstTopologyUpdate = singleTopologyUpdateAsync(_lastKnownUrls, null);
                        }
                    }

                    topologyUpdate = _firstTopologyUpdate;
                }
            }

            topologyUpdate.get();
        } catch (InterruptedException | ExecutionException e) {
            synchronized (this) {
                if (_firstTopologyUpdate == topologyUpdate) {
                    _firstTopologyUpdate = null; // next request will raise it
                }
            }

            throw ExceptionsUtils.unwrapException(e);
        }
    }

    private void updateTopologyCallback() {
        Date time = new Date();
        if (time.getTime() - _lastReturnedResponse.getTime() <= Duration.ofMinutes(5).toMillis()) {
            return;
        }

        ServerNode serverNode;

        try {
            NodeSelector selector = _nodeSelector;
            if (selector == null) {
                return;
            }
            CurrentIndexAndNode preferredNode = selector.getPreferredNode();
            serverNode = preferredNode.currentNode;
        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("Couldn't get preferred node Topology from _updateTopologyTimer", e);
            }
            return;
        }

        UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(serverNode);
        updateParameters.setTimeoutInMs(0);
        updateParameters.setDebugTag("timer-callback");

        updateTopologyAsync(updateParameters)
                .exceptionally(ex -> {
                    if (logger.isInfoEnabled()) {
                        logger.info("Couldn't update topology from _updateTopologyTimer", ex);
                    }
                    return null;
                });
    }

    protected CompletableFuture<Void> singleTopologyUpdateAsync(String[] initialUrls, UUID applicationIdentifier) {
        return CompletableFuture.runAsync(() -> {
            if (_disposed) {
                return;
            }

            // fetch tag for each of the urls
            Topology topology = new Topology();
            topology.setNodes(new ArrayList<>());
            topology.setEtag(topologyEtag);

            for (String url : initialUrls) {
                ServerNode serverNode = new ServerNode();
                serverNode.setUrl(url);
                serverNode.setDatabase(_databaseName);

                try {
                    GetNodeInfoCommand command = new GetNodeInfoCommand();
                    execute(serverNode, null, command, false, null);

                    serverNode.setClusterTag(command.getResult().getNodeTag());
                    serverNode.setServerRole(command.getResult().getServerRole());
                } catch (AuthorizationException e) {
                    // auth exceptions will always happen, on all nodes
                    // so errors immediately
                    _lastKnownUrls = initialUrls;
                    throw e;
                } catch (DatabaseDoesNotExistException e) {
                    // Will happen on all node in the cluster,
                    // so errors immediately

                    _lastKnownUrls = initialUrls;
                    throw e;
                } catch (Exception e) {
                    serverNode.setClusterTag("!");

                }

                topology.getNodes().add(serverNode);

                updateNodeSelector(topology, true);
            }

            _lastKnownUrls = initialUrls;
        });
    }

    protected CompletableFuture<Void> firstTopologyUpdate(String[] inputUrls) {
        return firstTopologyUpdate(inputUrls, null);
    }

    @SuppressWarnings({"ConstantConditions"})
    protected CompletableFuture<Void> firstTopologyUpdate(String[] inputUrls, UUID applicationIdentifier) {
        final String[] initialUrls = validateUrls(inputUrls, certificate);

        ArrayList<Tuple<String, Exception>> list = new ArrayList<>();

        return CompletableFuture.runAsync(() -> {

            for (String url : initialUrls) {
                try {
                    ServerNode serverNode = new ServerNode();
                    serverNode.setUrl(url);
                    serverNode.setDatabase(_databaseName);
                    serverNode.setServerRole(ServerNode.Role.MEMBER);

                    UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(serverNode);
                    updateParameters.setTimeoutInMs(Integer.MAX_VALUE);
                    updateParameters.setDebugTag("first-topology-update");
                    updateParameters.setApplicationIdentifier(applicationIdentifier);

                    updateTopologyAsync(updateParameters).get();

                    initializeUpdateTopologyTimer();

                    _topologyTakenFromNode = serverNode;
                    return;
                } catch (Exception e) {

                    if (e instanceof ExecutionException && e.getCause() instanceof AuthorizationException) {
                        // auth exceptions will always happen, on all nodes
                        // so errors immediately
                        _lastKnownUrls = initialUrls;
                        throw (AuthorizationException) e.getCause();
                    }

                    if (e instanceof ExecutionException && e.getCause() instanceof DatabaseDoesNotExistException) {
                        // Will happen on all node in the cluster,
                        // so errors immediately
                        _lastKnownUrls = initialUrls;
                        throw (DatabaseDoesNotExistException) e.getCause();
                    }

                    list.add(Tuple.create(url, e));
                }
            }

            Topology topology = new Topology();
            topology.setEtag(topologyEtag);

            List<ServerNode> topologyNodes = getTopologyNodes();
            if (topologyNodes == null) {
                topologyNodes = Arrays.stream(initialUrls)
                        .map(url -> {
                            ServerNode serverNode = new ServerNode();
                            serverNode.setUrl(url);
                            serverNode.setDatabase(_databaseName);
                            serverNode.setClusterTag("!");
                            return serverNode;
                        }).collect(Collectors.toList());
            }

            topology.setNodes(topologyNodes);

            _nodeSelector = new NodeSelector(topology, _executorService);

            if (initialUrls != null && initialUrls.length > 0) {
                initializeUpdateTopologyTimer();
                return;
            }

            _lastKnownUrls = initialUrls;
            String details = list.stream().map(x -> x.first + " -> " + Optional.ofNullable(x.second).map(Throwable::getMessage).orElse("")).collect(Collectors.joining(", "));
            throwExceptions(details);
        }, _executorService);
    }

    protected void throwExceptions(String details) {
        throw new IllegalStateException("Failed to retrieve database topology from all known nodes" + System.lineSeparator() + details);
    }

    public static String[] validateUrls(String[] initialUrls, KeyStore certificate) {
        String[] cleanUrls = new String[initialUrls.length];
        boolean requireHttps = certificate != null;
        for (int index = 0; index < initialUrls.length; index++) {
            String url = initialUrls[index];
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("'" + url + "' is not a valid url");
            }

            cleanUrls[index] = StringUtils.stripEnd(url, "/");
            requireHttps |= url.startsWith("https://");
        }

        if (!requireHttps) {
            return cleanUrls;
        }

        for (String url : initialUrls) {
            if (!url.startsWith("http://")) {
                continue;
            }

            if (certificate != null) {
                throw new IllegalStateException("The url " + url + " is using HTTP, but a certificate is specified, which require us to use HTTPS");
            }
            throw new IllegalStateException("The url " + url + " is using HTTP, but other urls are using HTTPS, and mixing of HTTP and HTTPS is not allowed.");
        }

        return cleanUrls;
    }

    private void initializeUpdateTopologyTimer() {
        if (_updateTopologyTimer != null) {
            return;
        }

        synchronized (this) {
            if (_updateTopologyTimer != null) {
                return;
            }

            _updateTopologyTimer = new Timer(this::updateTopologyCallback, Duration.ofMinutes(1), Duration.ofMinutes(1), _executorService);
        }
    }

    public <TResult> void execute(ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command, boolean shouldRetry, SessionInfo sessionInfo) {
        execute(chosenNode, nodeIndex, command, shouldRetry, sessionInfo, null);
    }

    @SuppressWarnings({"ConstantConditions"})
    public <TResult> void execute(ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command, boolean shouldRetry, SessionInfo sessionInfo, Reference<HttpRequestBase> requestRef) {
        if (command.failoverTopologyEtag == INITIAL_TOPOLOGY_ETAG) {
            command.failoverTopologyEtag = INITIAL_TOPOLOGY_ETAG;
            if (_nodeSelector != null && _nodeSelector.getTopology() != null) {
                Topology topology = _nodeSelector.getTopology();
                if (topology.getEtag() != null) {
                    command.failoverTopologyEtag = topology.getEtag();
                }
            }
        }

        Reference<String> urlRef = new Reference<>();
        HttpRequestBase request = createRequest(chosenNode, command, urlRef);

        if (request == null) {
            return;
        }

        if (requestRef != null) {
            requestRef.value = request;
        }

        //noinspection SimplifiableConditionalExpression
        boolean noCaching = sessionInfo != null ? sessionInfo.isNoCaching() : false;

        Reference<String> cachedChangeVectorRef = new Reference<>();
        Reference<String> cachedValue = new Reference<>();

        try (HttpCache.ReleaseCacheItem cachedItem = getFromCache(command, !noCaching, urlRef.value, cachedChangeVectorRef, cachedValue)) {
            if (cachedChangeVectorRef.value != null) {
                if (tryGetFromCache(command, cachedItem, cachedValue.value)) {
                    return;
                }
            }

            setRequestHeaders(sessionInfo, cachedChangeVectorRef.value, request);

            command.numberOfAttempts = command.numberOfAttempts + 1;
            int attemptNum = command.numberOfAttempts;
            EventHelper.invoke(_onBeforeRequest, this, new BeforeRequestEventArgs(_databaseName, urlRef.value, request, attemptNum));

            CloseableHttpResponse response = sendRequestToServer(chosenNode, nodeIndex, command, shouldRetry, sessionInfo, request, urlRef.value);

            if (response == null) {
                return;
            }

            CompletableFuture<Void> refreshTask = refreshIfNeeded(chosenNode, response);

            command.statusCode = response.getStatusLine().getStatusCode();

            ResponseDisposeHandling responseDispose = ResponseDisposeHandling.AUTOMATIC;

            try {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
                    EventHelper.invoke(_onSucceedRequest, this, new SucceedRequestEventArgs(_databaseName, urlRef.value, response, request, attemptNum));

                    cachedItem.notModified();

                    try {
                        if (command.getResponseType() == RavenCommandResponseType.OBJECT) {
                            command.setResponse(cachedValue.value, true);
                        }
                    } catch (IOException e) {
                        throw ExceptionsUtils.unwrapException(e);
                    }

                    return;
                }

                if (response.getStatusLine().getStatusCode() >= 400) {
                    if (!handleUnsuccessfulResponse(chosenNode, nodeIndex, command, request, response, urlRef.value, sessionInfo, shouldRetry)) {
                        Header dbMissingHeader = response.getFirstHeader("Database-Missing");
                        if (dbMissingHeader != null && dbMissingHeader.getValue() != null) {
                            throw new DatabaseDoesNotExistException(dbMissingHeader.getValue());
                        }

                        throwFailedToContactAllNodes(command, request);
                    }
                    return; // we either handled this already in the unsuccessful response or we are throwing
                }

                EventHelper.invoke(_onSucceedRequest, this, new SucceedRequestEventArgs(_databaseName, urlRef.value, response, request, attemptNum));

                responseDispose = command.processResponse(cache, response, urlRef.value);
                _lastReturnedResponse = new Date();
            } finally {
                if (responseDispose == ResponseDisposeHandling.AUTOMATIC) {
                    IOUtils.closeQuietly(response, null);
                }

                try {
                    refreshTask.get();
                } catch (Exception e) {
                    //noinspection ThrowFromFinallyBlock
                    throw ExceptionsUtils.unwrapException(e);
                }
            }
        }
    }

    private CompletableFuture<Void> refreshIfNeeded(ServerNode chosenNode, CloseableHttpResponse response) {
        Boolean refreshTopology = Optional.ofNullable(HttpExtensions.getBooleanHeader(response, Constants.Headers.REFRESH_TOPOLOGY)).orElse(false);
        Boolean refreshClientConfiguration = Optional.ofNullable(HttpExtensions.getBooleanHeader(response, Constants.Headers.REFRESH_CLIENT_CONFIGURATION)).orElse(false);

        CompletableFuture<Boolean> refreshTask = CompletableFuture.completedFuture(false);
        CompletableFuture<Void> refreshClientConfigurationTask = CompletableFuture.completedFuture(null);

        if (refreshTopology) {
            UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(chosenNode);
            updateParameters.setTimeoutInMs(0);
            updateParameters.setDebugTag("refresh-topology-header");
            refreshTask = updateTopologyAsync(updateParameters);
        }

        if (refreshClientConfiguration) {
            refreshClientConfigurationTask = updateClientConfigurationAsync(chosenNode);
        }

        return CompletableFuture.allOf(refreshTask, refreshClientConfigurationTask);
    }

    private <TResult> CloseableHttpResponse sendRequestToServer(ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command,
                                                                boolean shouldRetry, SessionInfo sessionInfo, HttpRequestBase request, String url) {
        try {
            numberOfServerRequests.incrementAndGet();

            Duration timeout = ObjectUtils.firstNonNull(command.getTimeout(), _defaultTimeout);
            if (timeout != null) {
                AggressiveCacheOptions callingTheadAggressiveCaching = aggressiveCaching.get();

                CompletableFuture<CloseableHttpResponse> sendTask = CompletableFuture.supplyAsync(() -> {
                    AggressiveCacheOptions aggressiveCacheOptionsToRestore = aggressiveCaching.get();

                    try {
                        aggressiveCaching.set(callingTheadAggressiveCaching);
                        return send(chosenNode, command, sessionInfo, request);
                    } catch (IOException e) {
                        throw ExceptionsUtils.unwrapException(e);
                    } finally {
                        aggressiveCaching.set(aggressiveCacheOptionsToRestore);
                    }
                }, _executorService);

                try {
                    return sendTask.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw ExceptionsUtils.unwrapException(e);
                } catch (TimeoutException e) {
                    request.abort();

                    net.ravendb.client.exceptions.TimeoutException timeoutException = new net.ravendb.client.exceptions.TimeoutException("The request for " + request.getURI() + " failed with timeout after " + TimeUtils.durationToTimeSpan(timeout), e);
                    if (!shouldRetry) {
                        if (command.getFailedNodes() == null) {
                            command.setFailedNodes(new HashMap<>());
                        }

                        command.getFailedNodes().put(chosenNode, timeoutException);
                        throw timeoutException;
                    }

                    if (!handleServerDown(url, chosenNode, nodeIndex, command, request, null, timeoutException, sessionInfo, shouldRetry)) {
                        throwFailedToContactAllNodes(command, request);
                    }

                    return null;
                } catch (ExecutionException e) {
                    Throwable rootCause = ExceptionUtils.getRootCause(e);
                    if (rootCause instanceof IOException) {
                        throw (IOException) rootCause;
                    }

                    throw ExceptionsUtils.unwrapException(e);
                }
            } else {
                return send(chosenNode, command, sessionInfo, request);
            }
        } catch (IOException e) {
            if (!shouldRetry) {
                throw ExceptionsUtils.unwrapException(e);
            }

            if (!handleServerDown(url, chosenNode, nodeIndex, command, request, null, e, sessionInfo, shouldRetry)) {
                throwFailedToContactAllNodes(command, request);
            }

            return null;
        }
    }

    private <TResult> CloseableHttpResponse send(ServerNode chosenNode, RavenCommand<TResult> command, SessionInfo sessionInfo, HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = null;

        if (shouldExecuteOnAll(chosenNode, command)) {
            response = executeOnAllToFigureOutTheFastest(chosenNode, command);
        } else {
            response = command.send(getHttpClient(), request);
        }

        // PERF: The reason to avoid rechecking every time is that servers wont change so rapidly
        //       and therefore we dimish its cost by orders of magnitude just doing it
        //       once in a while. We dont care also about the potential race conditions that may happen
        //       here mainly because the idea is to have a lax mechanism to recheck that is at least
        //       orders of magnitude faster than currently.
        if (chosenNode.shouldUpdateServerVersion()) {
            String serverVersion = tryGetServerVersion(response);
            if (serverVersion != null) {
                chosenNode.updateServerVersion(serverVersion);

            }
        }

        lastServerVersion = chosenNode.getLastServerVersion();

        if (sessionInfo != null && sessionInfo.getLastClusterTransactionIndex() != null) {
            // if we reach here it means that sometime a cluster transaction has occurred against this database.
            // Since the current executed command can be dependent on that, we have to wait for the cluster transaction.
            // But we can't do that if the server is an old one.

            if (lastServerVersion == null || lastServerVersion.compareToIgnoreCase("4.1") < 0) {
                throw new ClientVersionMismatchException("The server on " + chosenNode.getUrl() + " has an old version and can't perform " +
                        "the command since this command dependent on a cluster transaction which this node doesn't support.");
            }
        }

        return response;
    }

    private void setRequestHeaders(SessionInfo sessionInfo, String cachedChangeVector, HttpRequest request) {
        if (cachedChangeVector != null) {
            request.addHeader(Constants.Headers.IF_NONE_MATCH, "\"" + cachedChangeVector + "\"");
        }

        if (!_disableClientConfigurationUpdates) {
            request.addHeader(Constants.Headers.CLIENT_CONFIGURATION_ETAG, "\"" + clientConfigurationEtag + "\"");
        }

        if (sessionInfo != null && sessionInfo.getLastClusterTransactionIndex() != null) {
            request.addHeader(Constants.Headers.LAST_KNOWN_CLUSTER_TRANSACTION_INDEX, sessionInfo.getLastClusterTransactionIndex().toString());
        }

        if (!_disableTopologyUpdates) {
            request.addHeader(_topologyHeaderName, "\"" + topologyEtag + "\"");
        }

        if (request.getFirstHeader(Constants.Headers.CLIENT_VERSION) == null) {
            request.addHeader(Constants.Headers.CLIENT_VERSION, RequestExecutor.CLIENT_VERSION);
        }
    }

    private <TResult> boolean tryGetFromCache(RavenCommand<TResult> command, HttpCache.ReleaseCacheItem cachedItem, String cachedValue) {
        AggressiveCacheOptions aggressiveCacheOptions = aggressiveCaching.get();
        if (aggressiveCacheOptions != null &&
                cachedItem.getAge().compareTo(aggressiveCacheOptions.getDuration()) < 0 &&
                (!cachedItem.getMightHaveBeenModified() || aggressiveCacheOptions.getMode() != AggressiveCacheMode.TRACK_CHANGES) &&
                command.canCacheAggressively()) {
            try {
                if (cachedItem.item.flags.contains(ItemFlags.NOT_FOUND)) {
                    // if this is a cached delete, we only respect it if it _came_ from an aggressively cached
                    // block, otherwise, we'll run the request again

                    if (cachedItem.item.flags.contains(ItemFlags.AGGRESSIVELY_CACHED)) {
                        command.setResponse(cachedValue, true);
                        return true;
                    }
                } else {
                    command.setResponse(cachedValue, true);
                    return true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private static String tryGetServerVersion(CloseableHttpResponse response) {
        Header serverVersionHeader = response.getFirstHeader(Constants.Headers.SERVER_VERSION);

        if (serverVersionHeader != null) {
            return serverVersionHeader.getValue();
        }

        return null;
    }


    private <TResult> void throwFailedToContactAllNodes(RavenCommand<TResult> command, HttpRequestBase request) {
        if (command.getFailedNodes() == null || command.getFailedNodes().size() == 0) { //precaution, should never happen at this point
            throw new IllegalStateException("Received unsuccessful response and couldn't recover from it. " +
                    "Also, no record of exceptions per failed nodes. This is weird and should not happen.");
        }

        if (command.getFailedNodes().size() == 1) {
            throw ExceptionsUtils.unwrapException(command.getFailedNodes().values().iterator().next());
        }

        String message = "Tried to send " + command.resultClass.getName() + " request via " + request.getMethod()
                + " " + request.getURI() + " to all configured nodes in the topology, none of the attempt succeeded." + System.lineSeparator();

        if (_topologyTakenFromNode != null) {
            message += "I was able to fetch " + _topologyTakenFromNode.getDatabase()
                    + " topology from " + _topologyTakenFromNode.getUrl() + "." + System.lineSeparator();
        }

        List<ServerNode> nodes = null;
        if (_nodeSelector != null && _nodeSelector.getTopology() != null) {
            nodes = _nodeSelector.getTopology().getNodes();
        }

        if (nodes == null) {
            message += "Topology is empty.";
        } else {
            message += "Topology: ";

            for (ServerNode node : nodes) {
                Exception exception = command.getFailedNodes().get(node);
                message += System.lineSeparator() +
                        "[Url: " + node.getUrl() + ", " +
                        "ClusterTag: " + node.getClusterTag() + ", " +
                        "ServerRole: " + node.getServerRole() + ", " +
                        "Exception: " + (exception != null ? exception.getMessage() : "No exception") + "]";

            }
        }

        throw new AllTopologyNodesDownException(message);
    }

    public boolean inSpeedTestPhase() {
        return Optional.ofNullable(_nodeSelector).map(NodeSelector::inSpeedTestPhase).orElse(false);
    }

    private <TResult> boolean shouldExecuteOnAll(ServerNode chosenNode, RavenCommand<TResult> command) {
        return conventions.getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE &&
                _nodeSelector != null &&
                _nodeSelector.inSpeedTestPhase() &&
                Optional.ofNullable(_nodeSelector)
                        .map(NodeSelector::getTopology)
                        .map(Topology::getNodes)
                        .map(x -> x.size() > 1)
                        .orElse(false) &&
                command.isReadRequest() &&
                command.getResponseType() == RavenCommandResponseType.OBJECT &&
                chosenNode != null &&
                !(command instanceof IBroadcast);
    }

    @SuppressWarnings("ConstantConditions")
    private <TResult> CloseableHttpResponse executeOnAllToFigureOutTheFastest(ServerNode chosenNode, RavenCommand<TResult> command) {
        AtomicInteger numberOfFailedTasks = new AtomicInteger();

        CompletableFuture<IndexAndResponse> preferredTask = null;

        List<ServerNode> nodes = _nodeSelector.getTopology().getNodes();
        List<CompletableFuture<IndexAndResponse>> tasks = new ArrayList<>(Collections.nCopies(nodes.size(), null));

        for (int i = 0; i < nodes.size(); i++) {
            final int taskNumber = i;
            numberOfServerRequests.incrementAndGet();

            CompletableFuture<IndexAndResponse> task = CompletableFuture.supplyAsync(() -> {
                try {
                    Reference<String> strRef = new Reference<>();
                    HttpRequestBase request = createRequest(nodes.get(taskNumber), command, strRef);
                    setRequestHeaders(null, null, request);
                    return new IndexAndResponse(taskNumber, command.send(getHttpClient(), request));
                } catch (Exception e){
                    numberOfFailedTasks.incrementAndGet();
                    tasks.set(taskNumber, null);
                    throw new RuntimeException("Request execution failed", e);
                }
            }, _executorService);

            if (nodes.get(i).getClusterTag().equals(chosenNode.getClusterTag())) {
                preferredTask = task;
            } else {
                task.thenAcceptAsync(result -> IOUtils.closeQuietly(result.response, null));
            }

            tasks.set(i, task);
        }

        while (numberOfFailedTasks.get() < tasks.size()) {
            try {
                IndexAndResponse fastest = (IndexAndResponse) CompletableFuture
                        .anyOf(tasks.stream().filter(Objects::nonNull)
                        .toArray(CompletableFuture[]::new))
                        .get();
                _nodeSelector.recordFastest(fastest.index, nodes.get(fastest.index));
                break;
            } catch (InterruptedException | ExecutionException e) {
                for (int i = 0; i < nodes.size(); i++) {
                    if (tasks.get(i).isCompletedExceptionally()) {
                        numberOfFailedTasks.incrementAndGet();
                        tasks.set(i, null);
                    }
                }
            }
        }

        // we can reach here if the number of failed task equal to the number
        // of the nodes, in which case we have nothing to do

        try {
            return preferredTask.get().response;
        } catch (InterruptedException | ExecutionException e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }

    private <TResult> HttpCache.ReleaseCacheItem getFromCache(RavenCommand<TResult> command, boolean useCache, String url, Reference<String> cachedChangeVector, Reference<String> cachedValue) {
        if (useCache && command.canCache() && command.isReadRequest() && command.getResponseType() == RavenCommandResponseType.OBJECT) {
            return cache.get(url, cachedChangeVector, cachedValue);
        }

        cachedChangeVector.value = null;
        cachedValue.value = null;
        return new HttpCache.ReleaseCacheItem();
    }

    private <TResult> HttpRequestBase createRequest(ServerNode node, RavenCommand<TResult> command, Reference<String> url) {
        try {
            HttpRequestBase request = command.createRequest(node, url);
            if (request == null) {
                return null;
            }
            URI builder = new URI(url.value);

            if (requestPostProcessor != null) {
                requestPostProcessor.accept(request);
            }

            if (command instanceof IRaftCommand) {
                IRaftCommand raftCommand = (IRaftCommand) command;

                String raftRequestString = "raft-request-id=" + raftCommand.getRaftUniqueRequestId();

                builder = new URI(builder.getQuery() != null ? builder.toString() + "&" + raftRequestString : builder.toString() + "?" + raftRequestString);
            }

            if (shouldBroadcast(command)) {
                command.setTimeout(ObjectUtils.firstNonNull(command.getTimeout(), _firstBroadcastAttemptTimeout));
            }

            request.setURI(builder);

            return request;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse URL", e);
        }
    }

    private <TResult> boolean handleUnsuccessfulResponse(ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command, HttpRequestBase request, CloseableHttpResponse response, String url, SessionInfo sessionInfo, boolean shouldRetry) {
        try {
            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_NOT_FOUND:
                    cache.setNotFound(url, aggressiveCaching.get() != null);
                    switch (command.getResponseType()) {
                        case EMPTY:
                            return true;
                        case OBJECT:
                            command.setResponse(null, false);
                            break;
                        default:
                            command.setResponseRaw(response, null);
                            break;
                    }
                    return true;

                case HttpStatus.SC_FORBIDDEN:
                    String msg = tryGetResponseOfError(response);
                    StringBuilder builder = new StringBuilder("Forbidden access to ");
                    builder.append(chosenNode.getDatabase())
                            .append("@")
                            .append(chosenNode.getUrl())
                            .append(", ");

                    if (certificate == null) {
                        builder.append("a certificate is required. ");
                    } else {
                        builder.append("certificate does not have permission to access it or is unknown. ");
                    }

                    builder.append(" Method: ")
                        .append(request.getMethod())
                            .append(", Request: ")
                            .append(request.getURI().toString())
                            .append(System.lineSeparator())
                            .append(msg);

                    throw new AuthorizationException(builder.toString());

                case HttpStatus.SC_GONE: // request not relevant for the chosen node - the database has been moved to a different one
                    if (!shouldRetry) {
                        return false;
                    }

                    if (nodeIndex != null) {
                        _nodeSelector.onFailedRequest(nodeIndex);
                    }

                    if (command.getFailedNodes() == null) {
                        command.setFailedNodes(new HashMap<>());
                    }

                    if (!command.isFailedWithNode(chosenNode)) {
                        command.getFailedNodes().put(chosenNode, new UnsuccessfulRequestException("Request to " + request.getURI() + " (" + request.getMethod() + ") is not relevant for this node anymore."));
                    }

                    CurrentIndexAndNode indexAndNode = chooseNodeForRequest(command, sessionInfo);

                    if (command.getFailedNodes().containsKey(indexAndNode.currentNode)) {
                        // we tried all the nodes, let's try to update topology and retry one more time
                        UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(chosenNode);
                        updateParameters.setTimeoutInMs(60_000);
                        updateParameters.setForceUpdate(true);
                        updateParameters.setDebugTag("handle-unsuccessful-response");
                        Boolean success = updateTopologyAsync(updateParameters).get();
                        if (!success) {
                            return false;
                        }

                        command.getFailedNodes().clear(); // we just update the topology
                        indexAndNode = chooseNodeForRequest(command, sessionInfo);

                        execute(indexAndNode.currentNode, indexAndNode.currentIndex, command, false, sessionInfo);
                        return true;
                    }

                    execute(indexAndNode.currentNode, indexAndNode.currentIndex, command, false, sessionInfo);
                    return true;
                case HttpStatus.SC_GATEWAY_TIMEOUT:
                case HttpStatus.SC_REQUEST_TIMEOUT:
                case HttpStatus.SC_BAD_GATEWAY:
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    return handleServerDown(url, chosenNode, nodeIndex, command, request, response, null, sessionInfo, shouldRetry);
                case HttpStatus.SC_CONFLICT:
                    handleConflict(response);
                    break;
                case 425: // TooEarly
                    if (!shouldRetry) {
                        return false;
                    }

                    if (nodeIndex != null) {
                        _nodeSelector.onFailedRequest(nodeIndex);
                    }

                    if (command.getFailedNodes() == null) {
                        command.setFailedNodes(new HashMap<>());
                    }

                    if (!command.isFailedWithNode(chosenNode)) {
                        command.getFailedNodes().put(chosenNode,
                                new UnsuccessfulRequestException("Request to '" + request.getURI() + "' ("  +request.getMethod() + ") is processing and not yet available on that node."));
                    }

                    CurrentIndexAndNode nextNode = chooseNodeForRequest(command, sessionInfo);
                    execute(nextNode.currentNode, nextNode.currentIndex, command, true, sessionInfo);

                    _nodeSelector.restoreNodeIndex(chosenNode);

                    return true;
                default:
                    command.onResponseFailure(response);
                    ExceptionDispatcher.throwException(response);
                    break;
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw ExceptionsUtils.unwrapException(e);
        }

        return false;
    }

    private static String tryGetResponseOfError(CloseableHttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "Could not read request: " + e.getMessage();
        }
    }

    private static void handleConflict(CloseableHttpResponse response) {
        ExceptionDispatcher.throwException(response);
    }

    public static InputStream readAsStream(CloseableHttpResponse response) throws IOException {
        return response.getEntity().getContent();
    }

    private <TResult> boolean handleServerDown(String url, ServerNode chosenNode, Integer nodeIndex,
                                               RavenCommand<TResult> command, HttpRequestBase request,
                                               CloseableHttpResponse response, Exception e,
                                               SessionInfo sessionInfo, boolean shouldRetry) {
        if (command.getFailedNodes() == null) {
            command.setFailedNodes(new HashMap<>());
        }

        Exception exception = readExceptionFromServer(request, response, e);
        if (exception instanceof RavenTimeoutException && ((RavenTimeoutException) exception).isFailImmediately()) {
            throw (RavenTimeoutException)exception;
        }

        command.getFailedNodes().put(chosenNode, exception);

        if (nodeIndex == null) {
            //We executed request over a node not in the topology. This means no failover...
            return false;
        }

        if (_nodeSelector == null) {
            spawnHealthChecks(chosenNode, nodeIndex);
            return false;
        }

        // As the server is down, we discard the server version to ensure we update when it goes up.
        chosenNode.discardServerVersion();

        _nodeSelector.onFailedRequest(nodeIndex);

        if (shouldBroadcast(command)) {
            command.setResult(broadcast(command, sessionInfo));
            return true;
        }

        spawnHealthChecks(chosenNode, nodeIndex);

        CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, sessionInfo);
        long topologyEtag = _nodeSelector.getTopology() != null && _nodeSelector.getTopology().getEtag() != null ? _nodeSelector.getTopology().getEtag() : -2;
        if (command.failoverTopologyEtag != topologyEtag) {
            command.getFailedNodes().clear();
            command.failoverTopologyEtag = topologyEtag;
        }

        if (command.getFailedNodes().containsKey(currentIndexAndNode.currentNode)) {
            return false;
        }

        onFailedRequestInvoke(url, e, request, response);

        execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, shouldRetry, sessionInfo);

        return true;
    }

    private <TResult> boolean shouldBroadcast(RavenCommand<TResult> command) {
        if (!(command instanceof IBroadcast)) {
            return false;
        }

        List<ServerNode> topologyNodes = getTopologyNodes();

        if (topologyNodes == null || topologyNodes.size() < 2) {
            return false;
        }

        return true;
    }

    public static class BroadcastState<TResult> {
        private RavenCommand<TResult> command;
        private int index;
        private ServerNode node;
        private HttpRequestBase request;

        public RavenCommand<TResult> getCommand() {
            return command;
        }

        public void setCommand(RavenCommand<TResult> command) {
            this.command = command;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public ServerNode getNode() {
            return node;
        }

        public void setNode(ServerNode node) {
            this.node = node;
        }

        public HttpRequestBase getRequest() {
            return request;
        }

        public void setRequest(HttpRequestBase request) {
            this.request = request;
        }
    }

    private <TResult> TResult broadcast(RavenCommand<TResult> command, SessionInfo sessionInfo) {
        if (!(command instanceof IBroadcast)) {
            throw new IllegalStateException("You can broadcast only commands that implement 'IBroadcast'.");
        }

        IBroadcast broadcastCommand = (IBroadcast) command;
        final Map<ServerNode, Exception> failedNodes = command.getFailedNodes();

        command.setFailedNodes(new HashMap<>()); // clean the current failures
        Map<CompletableFuture<Void>, BroadcastState<TResult>> broadcastTasks = new HashMap<>();

        try {
            sendToAllNodes(broadcastTasks, sessionInfo, broadcastCommand);

            return waitForBroadcastResult(command, broadcastTasks);
        } finally {
            for (Map.Entry<CompletableFuture<Void>, BroadcastState<TResult>> broadcastState : broadcastTasks.entrySet()) {
                CompletableFuture<Void> task = broadcastState.getKey();
                if (task != null) {
                    task.exceptionally(throwable -> {
                        int index = broadcastState.getValue().getIndex();
                        ServerNode node = _nodeSelector.getTopology().getNodes().get(index);
                        if (failedNodes.containsKey(node)) {
                            // if other node succeed in broadcast we need to send health checks to the original failed node
                            spawnHealthChecks(node, index);
                        }
                        return null;
                    });
                }
            }
        }
    }

    private <TResult> TResult waitForBroadcastResult(RavenCommand<TResult> command, Map<CompletableFuture<Void>, BroadcastState<TResult>> tasks) {
        while (!tasks.isEmpty()) {
            Exception error = null;
            try {
                CompletableFuture.anyOf(tasks.keySet().toArray(new CompletableFuture[0])).get();
            } catch (InterruptedException | ExecutionException e) {
                error = e;
            }

            CompletableFuture<Void> completed = tasks
                    .keySet()
                    .stream()
                    .filter(CompletableFuture::isDone)
                    .findFirst()
                    .orElse(null);

            if (error != null) {
                BroadcastState<TResult> failed = tasks.get(completed);
                ServerNode node = _nodeSelector.getTopology().getNodes().get(failed.index);

                command.getFailedNodes().put(node, error.getCause() != null ? (Exception) error.getCause() : error);

                _nodeSelector.onFailedRequest(failed.getIndex());
                spawnHealthChecks(node, failed.getIndex());

                tasks.remove(completed);
                continue;
            }

            for (BroadcastState<TResult> state : tasks.values()) {
                HttpRequestBase request = state.getRequest();
                if (request != null) {
                    request.abort();
                }
            }

            _nodeSelector.restoreNodeIndex(tasks.get(completed).getNode());
            return tasks.get(completed).getCommand().getResult();
        }

        String exceptions = command
                .getFailedNodes()
                .entrySet()
                .stream()
                .map(x -> new UnsuccessfulRequestException(x.getKey().getUrl(), x.getValue()))
                .map(Throwable::toString)
                .collect(Collectors.joining(", "));

        throw new AllTopologyNodesDownException("Broadcasting " + command.getClass().getSimpleName() + " failed: " + exceptions);
    }

    @SuppressWarnings("unchecked")
    private <TResult> void sendToAllNodes(Map<CompletableFuture<Void>, BroadcastState<TResult>> tasks, SessionInfo sessionInfo, IBroadcast command) {
        for (int index = 0; index < _nodeSelector.getTopology().getNodes().size(); index++) {
            BroadcastState<TResult> state = new BroadcastState<>();
            state.setCommand((RavenCommand<TResult>)command.prepareToBroadcast(getConventions()));
            state.setIndex(index);
            state.setNode(_nodeSelector.getTopology().getNodes().get(index));

            state.getCommand().setTimeout(_secondBroadcastAttemptTimeout);

            AggressiveCacheOptions callingTheadAggressiveCaching = aggressiveCaching.get();

            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                AggressiveCacheOptions aggressiveCacheOptionsToRestore = aggressiveCaching.get();

                aggressiveCaching.set(callingTheadAggressiveCaching);
                try {
                    Reference<HttpRequestBase> requestRef = new Reference<>();
                    execute(state.getNode(), null, state.getCommand(), false, sessionInfo, requestRef);
                    state.setRequest(requestRef.value);
                } finally {
                    aggressiveCaching.set(aggressiveCacheOptionsToRestore);
                }
            }, _executorService);
            tasks.put(task, state);
        }
    }

    public ServerNode handleServerNotResponsive(String url, ServerNode chosenNode, int nodeIndex, Exception e) {
        spawnHealthChecks(chosenNode, nodeIndex);
        if (_nodeSelector != null) {
            _nodeSelector.onFailedRequest(nodeIndex);
        }
        CurrentIndexAndNode preferredNode = getPreferredNode();

        if (_disableTopologyUpdates) {
            performHealthCheck(chosenNode, nodeIndex);
        } else {
            try {
                UpdateTopologyParameters updateParameters = new UpdateTopologyParameters(preferredNode.currentNode);
                updateParameters.setTimeoutInMs(0);
                updateParameters.setForceUpdate(true);
                updateParameters.setDebugTag("handle-server-not-responsive");
                updateTopologyAsync(updateParameters).get();
            } catch (InterruptedException | ExecutionException ee) {
                throw ExceptionsUtils.unwrapException(e);
            }
        }

        onFailedRequestInvoke(url, e);

        return preferredNode.currentNode;
    }

    private void spawnHealthChecks(ServerNode chosenNode, int nodeIndex) {
        if (_nodeSelector != null && _nodeSelector.getTopology().getNodes().size() < 2) {
            return;
        }

        NodeStatus nodeStatus = new NodeStatus(this, nodeIndex, chosenNode);

        if (_failedNodesTimers.putIfAbsent(chosenNode, nodeStatus) == null) {
            nodeStatus.startTimer();
        }
    }

    private void checkNodeStatusCallback(NodeStatus nodeStatus) {
        // In some cases, race conditions may occur with a recently changed topology and a failed node.
        // We still should check the node's health, and if healthy, remove its timer and restore its index.

        int nodeIndex;
        ServerNode serverNode;
        NodeStatus status;

        try {
            CurrentIndexAndNode nodeIndexAndServerNode = _nodeSelector.getRequestedNode(nodeStatus.node.getClusterTag());
            nodeIndex = nodeIndexAndServerNode.currentIndex;
            serverNode = nodeIndexAndServerNode.currentNode;
        } catch (DatabaseDoesNotExistException | RequestedNodeUnavailableException e) {
            // There are no nodes in the topology or could not find requested node. Nothing we can do here

            status = _failedNodesTimers.get(nodeStatus.node);
            if (status != null) {
                status.close();
            }

            return;
        }

        if (serverNode == null) {
            return;
        }

        try {
            try {
                performHealthCheck(serverNode, nodeIndex);
            } catch (Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info(serverNode.getClusterTag() + " is still down", e);
                }

                status = _failedNodesTimers.get(nodeStatus.node);
                if (status != null) {
                    status.updateTimer();
                }

                return; // will wait for the next timer call
            }

            status = _failedNodesTimers.get(nodeStatus.node);
            if (status != null) {
                _failedNodesTimers.remove(nodeStatus.node);
                status.close();
            }

            if (_nodeSelector != null) {
                _nodeSelector.restoreNodeIndex(serverNode);
            }

        } catch (Exception e) {
            if (logger.isInfoEnabled()) {
                logger.info("Failed to check node topology, will ignore this node until next topology update", e);
            }
        }
    }

    protected void performHealthCheck(ServerNode serverNode, int nodeIndex) {
        try {
            if (!_useOldFailureCheckOperation.contains(serverNode.getUrl())) {
                execute(serverNode, nodeIndex, failureCheckOperation.getCommand(conventions), false, null);
            } else {
                executeOldHealthCheck(serverNode, nodeIndex);
            }
        } catch (Exception e) {
            if (e.getMessage().contains("RouteNotFoundException")) {
                _useOldFailureCheckOperation.add(serverNode.getUrl());
                executeOldHealthCheck(serverNode, nodeIndex);
                return;
            }

            throw ExceptionsUtils.unwrapException(e);
        }
    }

    private void executeOldHealthCheck(ServerNode serverNode, int nodeIndex) {
        execute(serverNode, nodeIndex, backwardCompatibilityFailureCheckOperation.getCommand(conventions), false, null);
    }

    private static <TResult> Exception readExceptionFromServer(HttpRequestBase request, CloseableHttpResponse response, Exception e) {
        if (response != null && response.getEntity() != null) {
            String responseJson = null;
            try {
                responseJson = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                return ExceptionDispatcher.get(JsonExtensions.getDefaultMapper().readValue(responseJson, ExceptionDispatcher.ExceptionSchema.class), response.getStatusLine().getStatusCode(), e);
            } catch (Exception __) {
                ExceptionDispatcher.ExceptionSchema exceptionSchema = new ExceptionDispatcher.ExceptionSchema();
                exceptionSchema.setUrl(request.getURI().toString());
                exceptionSchema.setMessage("Get unrecognized response from the server");
                exceptionSchema.setError(responseJson);
                exceptionSchema.setType("Unparsable Server Response");

                return ExceptionDispatcher.get(exceptionSchema, response.getStatusLine().getStatusCode(), e);
            }
        }

        // this would be connections that didn't have response, such as "couldn't connect to remote server"
        ExceptionDispatcher.ExceptionSchema exceptionSchema = new ExceptionDispatcher.ExceptionSchema();
        exceptionSchema.setUrl(request.getURI().toString());
        exceptionSchema.setMessage(e.getMessage());
        exceptionSchema.setError("An exception occurred while contacting " + request.getURI() + "." + System.lineSeparator() + e.toString());
        exceptionSchema.setType(e.getClass().getCanonicalName());

        return ExceptionDispatcher.get(exceptionSchema, HttpStatus.SC_SERVICE_UNAVAILABLE, e);
    }

    protected CompletableFuture<Void> _firstTopologyUpdate;
    protected String[] _lastKnownUrls;
    protected boolean _disposed;

    @Override
    public void close() {
        if (_disposed) {
            return;
        }

        _disposed = true;
        cache.close();

        if (_updateTopologyTimer != null) {
            _updateTopologyTimer.close();
        }
        
        disposeAllFailedNodesTimers();
    }

    private CloseableHttpClient createClient() {
        HttpClientBuilder httpClientBuilder = HttpClients
                .custom()
                .setMaxConnPerRoute(30)
                .setMaxConnTotal(40)
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectionRequestTimeout(3000)
                                .build()
                );

        if (conventions.hasExplicitlySetCompressionUsage() && !conventions.isUseCompression()) {
            httpClientBuilder.disableContentCompression();
        }

        httpClientBuilder
                .setRetryHandler(new StandardHttpRequestRetryHandler(0, false))
                .setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).build());

        if (certificate != null) {
            try {
                httpClientBuilder.setSSLHostnameVerifier((s, sslSession) -> {
                    // Here we are explicitly ignoring trust issues in the case of ClusterRequestExecutor.
                    // this is because we don't actually require trust, we just use the certificate
                    // as a way to authenticate. Either we encounter the same server certificate which we already
                    // trust, or the admin is going to tell us which specific certs we can trust.
                    return true;
                });

                httpClientBuilder.setSSLContext(createSSLContext());
            } catch ( Exception e) {
                throw new IllegalStateException("Unable to configure ssl context: " + e.getMessage(), e);
            }
        }

        if (configureHttpClient != null) {
            configureHttpClient.accept(httpClientBuilder);
        }

        return httpClientBuilder.build();
    }

    public SSLContext createSSLContext() throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadKeyMaterial(certificate, keyPassword);

        if (this.trustStore != null) {
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }

        return sslContextBuilder.build();
    }

    public static class NodeStatus implements CleanCloseable {

        private Duration _timerPeriod;
        private final RequestExecutor _requestExecutor;
        public final int nodeIndex;
        public final ServerNode node;
        private Timer _timer;

        public NodeStatus(RequestExecutor requestExecutor, int nodeIndex, ServerNode node) {
            _requestExecutor = requestExecutor;
            this.nodeIndex = nodeIndex;
            this.node = node;
            _timerPeriod = Duration.ofMillis(100);
        }

        private Duration nextTimerPeriod() {
            if (_timerPeriod.compareTo(Duration.ofSeconds(5)) >= 0) {
                return Duration.ofSeconds(5);
            }

            _timerPeriod = _timerPeriod.plus(Duration.ofMillis(100));

            return _timerPeriod;
        }

        public void startTimer() {
            _timer = new Timer(this::timerCallback, _timerPeriod, _requestExecutor._executorService);
        }

        private void timerCallback() {
            if (_requestExecutor._disposed) {
                close();
                return;
            }

            _requestExecutor.checkNodeStatusCallback(this);
        }

        public void updateTimer() {
            _timer.change(nextTimerPeriod());
        }

        @Override
        public void close() {
            _timer.close();
        }
    }

    public CurrentIndexAndNode getRequestedNode(String nodeTag) {
        ensureNodeSelector();

        return _nodeSelector.getRequestedNode(nodeTag);
    }

    public CurrentIndexAndNode getRequestedNode(String nodeTag, boolean throwIfContainsFailures) {
        CurrentIndexAndNode currentIndexAndNode = getRequestedNode(nodeTag);

        if (throwIfContainsFailures && !_nodeSelector.nodeIsAvailable(currentIndexAndNode.currentIndex)) {
            throw new RequestedNodeUnavailableException("Requested node " + nodeTag + " currently unavailable, please try again later.");
        }

        return currentIndexAndNode;
    }

    public CurrentIndexAndNode getPreferredNode() {
        ensureNodeSelector();

        return _nodeSelector.getPreferredNode();
    }

    public CurrentIndexAndNode getNodeBySessionId(int sessionId) {
        ensureNodeSelector();

        return _nodeSelector.getNodeBySessionId(sessionId);
    }

    public CurrentIndexAndNode getFastestNode() {
        ensureNodeSelector();

        return _nodeSelector.getFastestNode();
    }

    private void ensureNodeSelector() {
        if (!_disableTopologyUpdates) {
            waitForTopologyUpdate(_firstTopologyUpdate);
        }

        if (_nodeSelector == null) {
            Topology topology = new Topology();

            topology.setNodes(new ArrayList<>(getTopologyNodes()));
            topology.setEtag(topologyEtag);

            _nodeSelector = new NodeSelector(topology, _executorService);
        }
    }

    protected void onTopologyUpdatedInvoke(Topology newTopology) {
        EventHelper.invoke(_onTopologyUpdated, this, new TopologyUpdatedEventArgs(newTopology));
    }

    public static class IndexAndResponse {
        public final int index;
        public final CloseableHttpResponse response;

        public IndexAndResponse(int index, CloseableHttpResponse response) {
            this.index = index;
            this.response = response;
        }
    }
}
