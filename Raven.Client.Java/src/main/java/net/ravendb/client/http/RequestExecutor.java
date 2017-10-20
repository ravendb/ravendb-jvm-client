package net.ravendb.client.http;

import com.google.common.base.Stopwatch;
import com.sun.security.ntlm.Server;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.configuration.GetClientConfigurationOperation;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.serverwide.commands.GetTopologyCommand;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class RequestExecutor implements CleanCloseable {

    public static final String CLIENT_VERSION = "4.0.0";

    private static final ConcurrentMap<String, CloseableHttpClient> globalHttpClient = new ConcurrentHashMap<>(); //TODO: should we dispose this somewhere?

    private static final Duration GLOBAL_HTTP_CLIENT_TIMEOUT = Duration.ofHours(12);

    private final Semaphore _updateTopologySemaphore = new Semaphore(1);

    private final Semaphore _updateClientConfigurationSemaphore = new Semaphore(1);

    private final ConcurrentMap<ServerNode, NodeStatus> _failedNodesTimers = new ConcurrentHashMap<>();

    // TODO: public X509Certificate2 Certificate { get; }

    private final String _databaseName;

    private static final Log logger = LogFactory.getLog(RequestExecutor.class);

    private Date _lastReturnedResponse;
    protected final ReadBalanceBehavior _readBalanceBehavior;

    private final HttpCache cache = new HttpCache();

    public HttpCache getCache() {
        return cache;
    }

    //TODO: public readonly AsyncLocal<AggressiveCacheOptions> AggressiveCaching = new AsyncLocal<AggressiveCacheOptions>();

    public Topology getTopology() {
        return _nodeSelector != null ? _nodeSelector.getTopology() : null;
    }

    private CloseableHttpClient httpClient;

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public List<ServerNode> getTopologyNodes() {
        return Optional.ofNullable(getTopology())
                .map(x -> x.getNodes())
                .map(x -> Collections.unmodifiableList(x))
                .orElse(null);
    }

    /* TODO:
    private Timer _updateTopologyTimer;
    */

    protected NodeSelector _nodeSelector;

    private Duration _defaultTimeout;

    public long numberOfServerRequests;

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

    public final DocumentConventions conventions;

    protected boolean _disableTopologyUpdates;

    protected boolean _disableClientConfigurationUpdates;

    public Duration getDefaultTimeout() {
        return _defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        if (defaultTimeout != null && defaultTimeout.toMillis() > GLOBAL_HTTP_CLIENT_TIMEOUT.toMillis()) {
            throw new IllegalArgumentException("Maximum request timeout is set to " + GLOBAL_HTTP_CLIENT_TIMEOUT.toMillis() + " but was " + defaultTimeout.toMillis());
        }

        this._defaultTimeout = defaultTimeout;
    }

    protected RequestExecutor(String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        _readBalanceBehavior = conventions.getReadBalanceBehavior();
        _databaseName = databaseName;
        // TODO: Certificate = certificate;

        _lastReturnedResponse = new Date();
        this.conventions = conventions.clone();

        String thumbprint = "";

        //TODO: if (certificate != null)    thumbprint = certificate.Thumbprint;

        httpClient = globalHttpClient.computeIfAbsent(thumbprint, (thumb) -> createClient());
    }

    public static RequestExecutor create(String[] urls, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        RequestExecutor executor = new RequestExecutor(databaseName, conventions);
        executor._firstTopologyUpdate = executor.firstTopologyUpdate(urls);
        return executor;
    }

    public static RequestExecutor createForSingleNodeWithConfigurationUpdates(String url, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        RequestExecutor executor = createForSingleNodeWithoutConfigurationUpdates(url, databaseName, conventions);
        executor._disableClientConfigurationUpdates = false;
        return executor;
    }

    public static RequestExecutor createForSingleNodeWithoutConfigurationUpdates(String url, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        final String[] initialUrls = validateUrls(new String[] { url });

        RequestExecutor executor = new RequestExecutor(databaseName, conventions);

        Topology topology = new Topology();
        topology.setEtag(-1L);

        ServerNode serverNode = new ServerNode();
        serverNode.setDatabase(databaseName);
        serverNode.setUrl(initialUrls[0]);
        topology.setNodes(Arrays.asList(serverNode));

        executor._nodeSelector = new NodeSelector(topology);
        executor.topologyEtag = -2;
        executor._disableTopologyUpdates = true;
        executor._disableClientConfigurationUpdates = true;

        return executor;
    }

    protected CompletableFuture<Void> updateClientConfigurationAsync() {
        if (_disposed) {
            CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            try {
                _updateTopologySemaphore.acquire();
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
                CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, null);
                execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, false, null);

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
        });
    }

    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout) {
        return updateTopologyAsync(node, timeout, false);
    }

    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout, boolean forceUpdate) {
        if (_disposed) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {

            //prevent double topology updates if execution takes too much time
            // --> in cases with transient issues
            try {
                boolean lockTaken = _updateTopologySemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
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

                GetTopologyCommand command = new GetTopologyCommand();
                execute(node, null, command, false, null);

                if (_nodeSelector == null) {
                    _nodeSelector = new NodeSelector(command.getResult());

                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                } else if (_nodeSelector.onUpdateTopology(command.getResult(), forceUpdate)) {
                    //TODO: disposeAllFailedNodesTimers();
                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                }

                topologyEtag = _nodeSelector.getTopology().getEtag();

            } finally {
                _updateTopologySemaphore.release();
            }

            return true;
        });

    }

        /* TODO:

        protected void DisposeAllFailedNodesTimers()
        {
            var oldFailedNodesTimers = _failedNodesTimers;
            _failedNodesTimers.Clear();

            foreach (var failedNodesTimers in oldFailedNodesTimers)
            {
                failedNodesTimers.Value.Dispose();
            }

        }
*/

    public <TResult> void execute(RavenCommand<TResult> command) {
        execute(command, null);
    }

    public <TResult> void execute(RavenCommand<TResult> command, SessionInfo sessionInfo) {
        CompletableFuture<Void> topologyUpdate = _firstTopologyUpdate;
        if (topologyUpdate != null && topologyUpdate.isDone() || _disableTopologyUpdates) {
            CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, sessionInfo);
            execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, true, sessionInfo);
            return;
        } else {
            unlikelyExecute(command, topologyUpdate, sessionInfo);
        }
    }

    public <TResult> CurrentIndexAndNode chooseNodeForRequest(RavenCommand<TResult> cmd, SessionInfo sessionInfo) {
        if (!cmd.isReadRequest()) {
            return _nodeSelector.getPreferredNode();
        }

        switch (_readBalanceBehavior) {
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
        try {
            if (topologyUpdate == null) {
                synchronized (this) {
                    if (_firstTopologyUpdate == null) {
                        if (_lastKnownUrls == null) {
                            throw new IllegalStateException("No known topology and no previously known one, cannot proceed, likely a bug");
                        }

                        _firstTopologyUpdate  = firstTopologyUpdate(_lastKnownUrls);
                    }

                    topologyUpdate = _firstTopologyUpdate;
                }
            }

            topologyUpdate.get();
        } catch (Exception e) {
            synchronized (this) {
                if (_firstTopologyUpdate == topologyUpdate) {
                    _firstTopologyUpdate = null; // next request will raise it
                }
            }

            throw new IllegalStateException(e);
        }

        CurrentIndexAndNode currentIndexAndNode = chooseNodeForRequest(command, sessionInfo);
        execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, true, sessionInfo);
    }

    /*
TODO:
    private void UpdateTopologyCallback(object _)
    {
        var time = DateTime.UtcNow;
        if (time - _lastReturnedResponse <= TimeSpan.FromMinutes(5))
            return;

        ServerNode serverNode;

        try
        {
            var preferredNode = _nodeSelector.GetPreferredNode();
            serverNode = preferredNode.Node;
        }
        catch (Exception e)
        {
            if (Logger.IsInfoEnabled)
                Logger.Info("Couldn't get preferred node Topology from _updateTopologyTimer task", e);
            return;
        }
        GC.KeepAlive(Task.Run(async () =>
        {
            try
            {
                await UpdateTopologyAsync(serverNode, 0).ConfigureAwait(false);
            }
            catch (Exception e)
            {
                if (Logger.IsInfoEnabled)
                    Logger.Info("Couldn't Update Topology from _updateTopologyTimer task", e);
            }
        }));
    }

*/
    protected CompletableFuture<Void> firstTopologyUpdate(String[] inputUrls) {
        final String[] initialUrls = validateUrls(inputUrls);

        ArrayList<Tuple<String, Exception>> list = new ArrayList<>();

        return CompletableFuture.runAsync(() -> {

            for (String url : initialUrls) {
                try {
                    ServerNode serverNode = new ServerNode();
                    serverNode.setUrl(url);
                    serverNode.setDatabase(_databaseName);

                    updateTopologyAsync(serverNode, Integer.MAX_VALUE).get(); //TODO: it may block!

                    initializeUpdateTopologyTimer();
                    return;
                } catch (Exception e) { //TODO: handle https
                    if (initialUrls.length == 0) {
                        _lastKnownUrls = initialUrls;
                        throw new IllegalStateException("Cannot get topology from server: " + url, e);
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

            _nodeSelector = new NodeSelector(topology);

            for (String url : initialUrls) {
                initializeUpdateTopologyTimer();
                return;
            }

            _lastKnownUrls = initialUrls;
            String details = list.stream().map(x -> x.first + " -> " + Optional.ofNullable(x.second).map(m -> m.getMessage()).orElse("")).collect(Collectors.joining(", "));
            throw new IllegalStateException("Failed to retrieve cluster topology from all known nodes" + System.lineSeparator() + details);
        });
    }

    protected static String[] validateUrls(String[] initialUrls) { //TODO: certificate

        String[] cleanUrls = new String[initialUrls.length];
        for (int index = 0; index < initialUrls.length; index++) {
            String url = initialUrls[index];
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The url '" + url + "' is not valid");
            }

            cleanUrls[index] = StringUtils.stripEnd(url, "/");
        }
        return cleanUrls;

        //TODO: handle https validation
    }

    private void initializeUpdateTopologyTimer() {
        /* TODO:
         if (_updateTopologyTimer != null)
                return;

            lock (this)
            {
                if (_updateTopologyTimer != null)
                    return;

                _updateTopologyTimer = new Timer(UpdateTopologyCallback, null, TimeSpan.FromMinutes(5), TimeSpan.FromMinutes(5));
            }
         */
    }


    public <TResult> void execute(ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command, boolean shouldRetry, SessionInfo sessionInfo) {
        Reference<String> urlRef = new Reference<>();
        HttpRequestBase request = createRequest(chosenNode, command, urlRef);

        Reference<String> cachedChangeVector = new Reference<>();
        Reference<Object> cachedValue = new Reference<>(); //TODO: use some type here!

        try (HttpCache.ReleaseCacheItem cachedItem = getFromCache(command, urlRef.value, cachedChangeVector, cachedValue)) {
            if (cachedChangeVector.value != null) {
                /* TODO
                var aggressiveCacheOptions = AggressiveCaching.Value;
                    if (aggressiveCacheOptions != null &&
                        cachedItem.Age < aggressiveCacheOptions.Duration &&
                        cachedItem.MightHaveBeenModified == false &&
                        command.CanCacheAggressively)
                    {
                        command.SetResponse(cachedValue, fromCache: true);
                        return;
                    }
                 */

                request.addHeader("If-None-Match", "\"" + cachedChangeVector.value + "\"");
            }

            if (!_disableClientConfigurationUpdates) {
                request.addHeader(Constants.Headers.CLIENT_CONFIGURATION_ETAG, "\"" + clientConfigurationEtag + "\"");
            }

            if (!_disableTopologyUpdates) {
                request.addHeader(Constants.Headers.TOPOLOGY_ETAG, "\"" + topologyEtag + "\"");
            }

            Stopwatch sp = Stopwatch.createStarted();
            CloseableHttpResponse response = null;
            ResponseDisposeHandling responseDispose = ResponseDisposeHandling.AUTOMATIC;

            try {
                numberOfServerRequests++; //TODO: interlocked?

                Duration timeout = command.getTimeout() != null ? command.getTimeout() : _defaultTimeout;

                if (timeout != null) {
                    throw new UnsupportedOperationException("TDODO");
                    /*
                    if (timeout > GlobalHttpClientTimeout)
                            ThrowTimeoutTooLarge(timeout);

                        using (var cts = CancellationTokenSource.CreateLinkedTokenSource(token, CancellationToken.None))
                        {
                            cts.CancelAfter(timeout.Value);
                            try
                            {
                                var preferredTask = command.SendAsync(HttpClient, request, cts.Token);
                                if (ShouldExecuteOnAll(chosenNode, command))
                                {
                                    await ExecuteOnAllToFigureOutTheFastest(chosenNode, command, preferredTask, cts.Token).ConfigureAwait(false);
                                }

                                response = await preferredTask.ConfigureAwait(false);
                            }
                            catch (OperationCanceledException e)
                            {
                                if (cts.IsCancellationRequested && token.IsCancellationRequested == false) // only when we timed out
                                {
                                    var timeoutException = new TimeoutException($"The request for {request.RequestUri} failed with timeout after {timeout}", e);
                                    if (shouldRetry == false)
                                        throw timeoutException;

                                    sp.Stop();
                                    if (sessionInfo != null)
                                    {
                                        sessionInfo.AsyncCommandRunning = false;
                                    }

                                    if (await HandleServerDown(url, chosenNode, nodeIndex, context, command, request, response, e, sessionInfo).ConfigureAwait(false) == false)
                                    {
                                        ThrowFailedToContactAllNodes(command, request, e, timeoutException);
                                    }

                                    return;
                                }
                                throw;
                            }
                        }
                     */
                } else {
                    CloseableHttpResponse preferredTask = command.send(httpClient, request); //TODO: it returns task!
                    if (shouldExecuteOnAll(chosenNode, command)) {
                        //TODO: await ExecuteOnAllToFigureOutTheFastest(chosenNode, command, preferredTask, token).ConfigureAwait(false);
                    }

                    response = preferredTask; //TODO: here we should await
                }

                sp.stop();
            } catch (IOException e) {
                if (!shouldRetry) {
                    throw new RuntimeException(e); //TODO: use better exception!
                }
                sp.stop();

                //TODO :if (await HandleServerDown(url, chosenNode, nodeIndex, context, command, request, response, e, sessionInfo).ConfigureAwait(false) == false)
                //TODO:    ThrowFailedToContactAllNodes(command, request, e, null);

                return;
            }

            command.statusCode = response.getStatusLine().getStatusCode();

            Boolean refreshTopology = Optional.ofNullable(HttpExtensions.getBooleanHeader(response, Constants.Headers.REFRESH_TOPOLOGY)).orElse(false);
            Boolean refreshClientConfiguration = Optional.ofNullable(HttpExtensions.getBooleanHeader(response, Constants.Headers.REFRESH_CLIENT_CONFIGURATION)).orElse(false);

            try {
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
                    cachedItem.notModified();

                    try {
                        if (command.getResponseType() == RavenCommandResponseType.OBJECT) {
                            command.setResponse((InputStream)cachedValue.value, true); //TODO: remove cast!
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e); //TODO: use better exception!
                    }

                    return;
                }

                if (response.getStatusLine().getStatusCode() >= 400) {
                    /* TODO
                    if (await HandleUnsuccessfulResponse(chosenNode, nodeIndex, context, command, request, response, url, sessionInfo, shouldRetry).ConfigureAwait(false) == false)
                        {
                            if (response.Headers.TryGetValues("Database-Missing", out var databaseMissing))
                            {
                                var name = databaseMissing.FirstOrDefault();
                                if (name != null)
                                    throw new DatabaseDoesNotExistException(name);
                            }

                            if (command.FailedNodes.Count == 0) //precaution, should never happen at this point
                                throw new InvalidOperationException("Received unsuccessful response and couldn't recover from it. Also, no record of exceptions per failed nodes. This is weird and should not happen.");

                            if (command.FailedNodes.Count == 1)
                            {
                                var node = command.FailedNodes.First();
                                throw node.Value;
                            }

                            throw new AllTopologyNodesDownException("Received unsuccessful response from all servers and couldn't recover from it.",
                                new AggregateException(command.FailedNodes.Select(x => new UnsuccessfulRequestException(x.Key.Url, x.Value))));
                        }
                        return; // we either handled this already in the unsuccessful response or we are throwing
                     */
                }

                responseDispose = command.processResponse(cache, response, urlRef.value);
                _lastReturnedResponse = new Date();
            } finally {
                if (responseDispose == ResponseDisposeHandling.AUTOMATIC) {
                    IOUtils.closeQuietly(response);
                }

                if (refreshTopology || refreshClientConfiguration) {

                    ServerNode serverNode = new ServerNode();
                    serverNode.setUrl(chosenNode.getUrl());
                    serverNode.setDatabase(_databaseName);

                    CompletableFuture<Boolean> topologyTask = refreshTopology ? updateTopologyAsync(serverNode, 0) : CompletableFuture.completedFuture(false);
                    CompletableFuture<Void> clientConfiguration = refreshClientConfiguration ? updateClientConfigurationAsync() : CompletableFuture.completedFuture(null);

                    try {
                        CompletableFuture.allOf(topologyTask, clientConfiguration).get();
                    } catch (Exception e) {
                        throw new RuntimeException(e); //TODO: better exception handling
                    }
                }
            }
        }
    }

    public void setTimeout(HttpRequestBase requestBase, long timeoutInMilis) {
        RequestConfig requestConfig = requestBase.getConfig();
        if (requestConfig == null) {
            requestConfig = RequestConfig.DEFAULT;
        }

        requestConfig = RequestConfig.copy(requestConfig).setSocketTimeout((int) timeoutInMilis).setConnectTimeout((int) timeoutInMilis).build();
        requestBase.setConfig(requestConfig);
    }

    /*

    private <TResult> void throwFailedToContactAllNodes(RavenCommand<TResult> command, HttpRequestBase request, Exception e, Exception timeoutException) {
        //tODO: throw new //TODO:
    }

    public boolean inSpeedTestPhase() {
        return _nodeSelector != null ? _nodeSelector.inSpeedTestPhase() : false;
    }

*/

    private <TResult> boolean shouldExecuteOnAll(ServerNode chosenNode, RavenCommand<TResult> command) {
        return _readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE &&
                _nodeSelector != null &&
                _nodeSelector.inSpeedTestPhase() &&
                Optional.ofNullable(_nodeSelector).map(x -> x.getTopology()).map(x -> x.getNodes()).map(x -> x.size() > 0).orElse(false) &&
                command.isReadRequest() &&
                command.getResponseType() == RavenCommandResponseType.OBJECT &&
                chosenNode != null;
    }

    /*
    private void ThrowFailedToContactAllNodes<TResult>(RavenCommand<TResult> command, HttpRequestMessage request, Exception e, Exception timeoutException)
    {
        throw new AllTopologyNodesDownException(
            $"Tried to send '{command.GetType().Name}' request via `{request.Method} {request.RequestUri.PathAndQuery}` to all configured nodes in the topology, all of them seem to be down or not responding. I've tried to access the following nodes: " +
            string.Join(",", _nodeSelector?.Topology.Nodes.Select(x => x.Url) ?? new string[0]), _nodeSelector?.Topology, timeoutException ?? e);
    }

    private static readonly Task<HttpRequestMessage> NeverEndingRequest = new TaskCompletionSource<HttpRequestMessage>().Task;

    private async Task ExecuteOnAllToFigureOutTheFastest<TResult>(ServerNode chosenNode, RavenCommand<TResult> command, Task<HttpResponseMessage> preferredTask,
        CancellationToken token = default(CancellationToken))
    {
        int numberOfFailedTasks = 0;

        var nodes = _nodeSelector.Topology.Nodes;
        var tasks = new Task[nodes.Count];
        for (int i = 0; i < nodes.Count; i++)
        {
            if (nodes[i].ClusterTag == chosenNode.ClusterTag)
            {
                tasks[i] = preferredTask;
                continue;
            }

            IDisposable disposable = null;

            try
            {
                disposable = ContextPool.AllocateOperationContext(out var tmpCtx);
                var request = CreateRequest(tmpCtx, nodes[i], command, out var _);

                Interlocked.Increment(ref NumberOfServerRequests);
                tasks[i] = command.SendAsync(HttpClient, request, token).ContinueWith(x =>
                {
                    try
                    {
                        if (x.Exception != null)
                        {
                            // we need to make sure that the response is
                            // properly disposed from all the calls
                            x.Result.Dispose();
                        }
                    }
                    catch (Exception)
                    {
                        // there is really nothing we can do here
                    }
                    finally
                    {
                        disposable?.Dispose();
                    }
                }, token);
            }
            catch (Exception)
            {
                numberOfFailedTasks++;
                // nothing we can do about it
                tasks[i] = NeverEndingRequest;
                disposable?.Dispose();
            }
        }

        while (numberOfFailedTasks < tasks.Length)
        {
            // here we rely on WhenAny NOT throwing if the completed
            // task has failed
            var completed = await Task.WhenAny(tasks).ConfigureAwait(false);
            var index = Array.IndexOf(tasks, completed);
            if (completed.IsCanceled || completed.IsFaulted)
            {
                tasks[index] = NeverEndingRequest;
                numberOfFailedTasks++;
                continue;
            }
            _nodeSelector.RecordFastest(index, nodes[index]);
            return;
        }
        // we can reach here if the number of failed task equal to the nuber
        // of the nodes, in which case we have nothing to do
    }

    private static void ThrowTimeoutTooLarge(TimeSpan? timeout)
    {
        throw new InvalidOperationException($"Maximum request timeout is set to '{GlobalHttpClientTimeout}' but was '{timeout}'.");
    }
    */

    private <TResult> HttpCache.ReleaseCacheItem getFromCache(RavenCommand<TResult> command, String url, Reference<String> cachedChangeVector, Reference<Object> cachedValue) { //TODO: avoid object here
        if (command.canCache() && command.isReadRequest() && command.getResponseType() == RavenCommandResponseType.OBJECT) {
            return cache.get(url, cachedChangeVector, cachedValue);
        }

        cachedChangeVector.value = null;
        cachedValue.value = null;
        return new HttpCache.ReleaseCacheItem();
    }

    private <TResult> HttpRequestBase createRequest(ServerNode node, RavenCommand<TResult> command, Reference<String> url) {
        try {
            HttpRequestBase request = command.createRequest(node, url);
            request.setURI(new URI(url.value));

        /* TODO
         if (!request.Headers.Contains("Raven-Client-Version"))
                request.Headers.Add("Raven-Client-Version", ClientVersion);
         */
            return request;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Unable to parse URL", e);
        }
    }

    /*

    public event Action<StringBuilder> AdditionalErrorInformation;

    private async Task<bool> HandleUnsuccessfulResponse<TResult>(ServerNode chosenNode, int? nodeIndex, JsonOperationContext context, RavenCommand<TResult> command, HttpRequestMessage request, HttpResponseMessage response, string url, SessionInfo sessionInfo, bool shouldRetry)
    {
        switch (response.StatusCode)
        {
            case HttpStatusCode.NotFound:
                Cache.SetNotFound(url);
                if (command.ResponseType == RavenCommandResponseType.Empty)
                    return true;
                else if (command.ResponseType == RavenCommandResponseType.Object)
                    command.SetResponse(null, fromCache: false);
                else
                    command.SetResponseRaw(response, null, context);
                return true;
            case HttpStatusCode.Forbidden:
                throw new AuthorizationException("Forbidden access to " + chosenNode.Database + "@" + chosenNode.Url + ", " +
                    (Certificate == null ? "a certificate is required." : Certificate.FriendlyName + " does not have permission to access it or is unknown.") +
                    request.Method + " " + request.RequestUri);
            case HttpStatusCode.Gone: // request not relevant for the chosen node - the database has been moved to a different one
                if (shouldRetry == false)
                    return false;

                await UpdateTopologyAsync(chosenNode, Timeout.Infinite, forceUpdate: true).ConfigureAwait(false);
                var (index, node) = ChooseNodeForRequest(command, sessionInfo);
                await ExecuteAsync(node, index, context, command, shouldRetry: false, sessionInfo: sessionInfo).ConfigureAwait(false);
                return true;
            case HttpStatusCode.GatewayTimeout:
            case HttpStatusCode.RequestTimeout:
            case HttpStatusCode.BadGateway:
            case HttpStatusCode.ServiceUnavailable:
                await HandleServerDown(url, chosenNode, nodeIndex, context, command, request, response, null, sessionInfo).ConfigureAwait(false);
                break;
            case HttpStatusCode.Conflict:
                await HandleConflict(context, response).ConfigureAwait(false);
                break;
            default:
                command.OnResponseFailure(response);
                await ExceptionDispatcher.Throw(context, response, AdditionalErrorInformation).ConfigureAwait(false);
                break;
        }
        return false;
    }

    private static Task HandleConflict(JsonOperationContext context, HttpResponseMessage response)
    {
        // TODO: Conflict resolution
        // current implementation is temporary

        return ExceptionDispatcher.Throw(context, response);
    }

    public static async Task<Stream> ReadAsStreamUncompressedAsync(HttpResponseMessage response)
    {
        var serverStream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false);
        var stream = serverStream;
        var encoding = response.Content.Headers.ContentEncoding.FirstOrDefault();
        if (encoding != null && encoding.Contains("gzip"))
            return new GZipStream(stream, CompressionMode.Decompress);
        if (encoding != null && encoding.Contains("deflate"))
            return new DeflateStream(stream, CompressionMode.Decompress);

        return serverStream;
    }
*/

    private <TResult> boolean handleServerDown(String url, ServerNode chosenNode, Integer nodeIndex, RavenCommand<TResult> command, HttpRequestBase request, CloseableHttpResponse response, Exception e, SessionInfo sessionInfo) {
        if (command.getFailedNodes() == null) {
            command.setFailedNodes(new HashMap<>());
        }

        addFailedResponseToCommand(chosenNode, command, request, response, e);

        if (nodeIndex == null) {
            //We executed request over a node not in the topology. This means no failover...
            return false;
        }

        //TODO:  SpawnHealthChecks(chosenNode, nodeIndex.Value);

        if (_nodeSelector == null) {
            return false;
        }

        _nodeSelector.onFailedRequest(nodeIndex);

        CurrentIndexAndNode currentIndexAndNode = _nodeSelector.getPreferredNode();
        if (command.getFailedNodes().containsKey(currentIndexAndNode.currentNode)) {
            return false; //we tried all the nodes...nothing left to do
        }

        execute(currentIndexAndNode.currentNode, currentIndexAndNode.currentIndex, command, false, sessionInfo);

        return true;
    }

    /*

    private void SpawnHealthChecks(ServerNode chosenNode, int nodeIndex)
    {
        var nodeStatus = new NodeStatus(this, nodeIndex, chosenNode);
        if (_failedNodesTimers.TryAdd(chosenNode, nodeStatus))
            nodeStatus.StartTimer();
    }

    private async Task CheckNodeStatusCallback(NodeStatus nodeStatus)
    {
        var copy = TopologyNodes;
        if (nodeStatus.NodeIndex >= copy.Count)
            return; // topology index changed / removed
        var serverNode = copy[nodeStatus.NodeIndex];
        if (ReferenceEquals(serverNode, nodeStatus.Node) == false)
            return; // topology changed, nothing to check

        try
        {
            using (ContextPool.AllocateOperationContext(out JsonOperationContext context))
            {
                NodeStatus status;
                try
                {
                    await PerformHealthCheck(serverNode, nodeStatus.NodeIndex, context).ConfigureAwait(false);
                }
                catch (Exception e)
                {
                    if (Logger.IsInfoEnabled)
                        Logger.Info($"{serverNode.ClusterTag} is still down", e);

                    if (_failedNodesTimers.TryGetValue(nodeStatus.Node, out status))
                        nodeStatus.UpdateTimer();

                    return;// will wait for the next timer call
                }

                if (_failedNodesTimers.TryRemove(nodeStatus.Node, out status))
                {
                    status.Dispose();
                }
                _nodeSelector?.RestoreNodeIndex(nodeStatus.NodeIndex);
            }
        }
        catch (Exception e)
        {
            if (Logger.IsInfoEnabled)
                Logger.Info("Failed to check node topology, will ignore this node until next topology update", e);
        }
    }

    protected virtual Task PerformHealthCheck(ServerNode serverNode, int nodeIndex, JsonOperationContext context)
    {
        return ExecuteAsync(serverNode, nodeIndex, context, new GetStatisticsCommand(debugTag: "failure=check"), shouldRetry: false);
    }

*/
    //TODO: make sure we dispose response in case of failure!
    private static <TResult> void addFailedResponseToCommand(ServerNode chosenNode, RavenCommand<TResult> command, HttpRequestBase request, CloseableHttpResponse respoonse, Exception e) {
        if (respoonse != null) {
            /* TODO
             var stream = await response.Content.ReadAsStreamAsync().ConfigureAwait(false);
            var ms = new MemoryStream();
            await stream.CopyToAsync(ms).ConfigureAwait(false);
            try
            {
                ms.Position = 0;
                using (var responseJson = context.ReadForMemory(ms, "RequestExecutor/HandleServerDown/ReadResponseContent"))
                {
                    command.FailedNodes.Add(chosenNode, ExceptionDispatcher.Get(JsonDeserializationClient.ExceptionSchema(responseJson), response.StatusCode));
                }
            }
            catch
            {
                // we failed to parse the error
                ms.Position = 0;
                command.FailedNodes.Add(chosenNode, ExceptionDispatcher.Get(new ExceptionDispatcher.ExceptionSchema
                {
                    Url = request.RequestUri.ToString(),
                    Message = "Got unrecognized response from the server",
                    Error = new StreamReader(ms).ReadToEnd(),
                    Type = "Unparseable Server Response"
                }, response.StatusCode));
            }
            return;
             */

        }

        // this would be connections that didn't have response, such as "couldn't connect to remote server"
        command.getFailedNodes().put(chosenNode, new Exception("TODO")); //TODO:
                /* TODO:
                ExceptionDispatcher.Get(new ExceptionDispatcher.ExceptionSchema
                {
                    Url = request.RequestUri.ToString(),
                    Message = e.Message,
                    Error = e.ToString(),
                    Type = e.GetType().FullName
                }, HttpStatusCode.InternalServerError));
            }
                 */
    }

    protected boolean _disposed;
    protected CompletableFuture<Void> _firstTopologyUpdate;
    protected String[] _lastKnownUrls;

    @Override
    public void close() {
        if (_disposed) {
            return;
        }

        try {
            _updateTopologySemaphore.acquire();
        } catch (InterruptedException e) {}

        if (_disposed) {
            return;
        }

        _disposed = true;
        cache.close();
        //TODO: _updateTopologyTimer?.Dispose();
        //TODO: disposeAllFailedNodesTimers();
    }

    private CloseableHttpClient createClient() {
        //TODO: certifciates handling, timeout: GlobalHttpClientTimeout?

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(10);
        return HttpClients
                .custom()
                .setConnectionManager(cm)
                .disableContentCompression()
                //TODO : .addInterceptorLast(new RavenResponseContentEncoding())
                .setRetryHandler(new StandardHttpRequestRetryHandler(0, false))
                .setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).build()).
                        build();
    }


    //TODO: ValidateClientKeyUsages

    public static class NodeStatus implements CleanCloseable {
        @Override
        public void close() {
            //TODO:
        }
    }

    /*


        public class NodeStatus : IDisposable
        {
            private TimeSpan _timerPeriod;
            private readonly RequestExecutor _requestExecutor;
            public readonly int NodeIndex;
            public readonly ServerNode Node;
            private Timer _timer;

            public NodeStatus(RequestExecutor requestExecutor, int nodeIndex, ServerNode node)
            {
                _requestExecutor = requestExecutor;
                NodeIndex = nodeIndex;
                Node = node;
                _timerPeriod = TimeSpan.FromMilliseconds(100);
            }

            private TimeSpan NextTimerPeriod()
            {
                if (_timerPeriod >= TimeSpan.FromSeconds(5))
                    return TimeSpan.FromSeconds(5);

                _timerPeriod += TimeSpan.FromMilliseconds(100);
                return _timerPeriod;
            }

            public void StartTimer()
            {
                _timer = new Timer(TimerCallback, null, _timerPeriod, Timeout.InfiniteTimeSpan);
            }

            private void TimerCallback(object state)
            {
                GC.KeepAlive(_requestExecutor.CheckNodeStatusCallback(this));
            }

            public void UpdateTimer()
            {
                Debug.Assert(_timer != null);
                _timer.Change(NextTimerPeriod(), Timeout.InfiniteTimeSpan);
            }

            public void Dispose()
            {
                _timer?.Dispose();
            }
        }

        public async Task<(int, ServerNode)> GetPreferredNode()
        {
            await EnsureNodeSelector().ConfigureAwait(false);

            return _nodeSelector.GetPreferredNode();
        }



     */


    private void ensureNodeSelector() throws ExecutionException, InterruptedException {
        if (!_firstTopologyUpdate.isDone()) {
            _firstTopologyUpdate.get();
        }

        if (_nodeSelector == null) {
            Topology topology = new Topology();

            topology.setNodes(getTopologyNodes());
            topology.setEtag(topologyEtag);

            _nodeSelector = new NodeSelector(topology);
        }
    }
}
