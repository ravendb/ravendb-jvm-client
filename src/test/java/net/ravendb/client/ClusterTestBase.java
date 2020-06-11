package net.ravendb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.driver.RavenServerLocator;
import net.ravendb.client.driver.RavenTestDriver;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.infrastructure.AdminJsConsoleOperation;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.commands.GetClusterTopologyCommand;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DatabasePutResult;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class ClusterTestBase extends RavenTestDriver implements CleanCloseable {

    private List<Closeable> _toDispose = new ArrayList<>();

    private static class TestCloudServiceLocator extends RavenServerLocator {

        private static Map<String, String> _defaultParams = new HashMap<>();
        private Map<String, String> _extraParams = new HashMap<>();

        static {
            _defaultParams.put("ServerUrl", "http://127.0.0.1:0");
            _defaultParams.put("Features.Availability", "Experimental");
        }

        public TestCloudServiceLocator() {
        }

        public TestCloudServiceLocator(Map<String, String> extraParams) {
            _extraParams = extraParams;
        }

        @Override
        public String[] getCommandArguments() {
            return Stream.concat(_defaultParams.entrySet().stream(), _extraParams.entrySet().stream())
                    .map(x -> "--" + x.getKey() + "=" + x.getValue())
                    .toArray(String[]::new);

        }
    }

    private AtomicInteger dbCounter = new AtomicInteger(1);

    protected String getDatabaseName() {
        return "db_" + dbCounter.incrementAndGet();
    }

    protected ClusterController createRaftCluster(int numberOfNodes) throws Exception {
        return createRaftCluster(numberOfNodes, new HashMap<>());
    }

    protected ClusterController createRaftCluster(int numberOfNodes, Map<String, String> customSettings) throws Exception {
        ClusterController cluster = new ClusterController();
        cluster.nodes = new ArrayList<>();

        String[] allowedNodeTags = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };

        int leaderIndex = 0;
        String leaderNodeTag = allowedNodeTags[leaderIndex];

        for (int i = 0; i < numberOfNodes; i++) {
            Reference<Process> processReference = new Reference<>();
            IDocumentStore store = runServerInternal(new TestCloudServiceLocator(customSettings), processReference, null);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> killProcess(processReference.value)));

            ClusterNode clusterNode = new ClusterNode();
            clusterNode.serverProcess = processReference.value;
            clusterNode.store = store;
            clusterNode.url = store.getUrls()[0];
            clusterNode.nodeTag = allowedNodeTags[i];
            clusterNode.leader = i == leaderIndex;

            cluster.nodes.add(clusterNode);
        }

        cluster.executeJsScript(leaderNodeTag,
                "server.ServerStore.EnsureNotPassive(null, \"" + leaderNodeTag + "\");");

        if (numberOfNodes > 1) {
            // add nodes to cluster
            for (int i = 0; i < numberOfNodes; i++) {
                if (i == leaderIndex) {
                    continue;
                }

                String nodeTag = allowedNodeTags[i];
                String url = cluster.nodes.get(i).url;
                cluster.executeJsScript(leaderNodeTag,
                        "server.ServerStore.ValidateFixedPort = false;" +
                                "server.ServerStore.AddNodeToClusterAsync(\"" + url + "\", \"" + nodeTag + "\", false, false, server.ServerStore.ServerShutdown).Wait();");

                cluster.executeJsScript(nodeTag,
                        "server.ServerStore.WaitForTopology(0, server.ServerStore.ServerShutdown).Wait();");
            }
        }

        return cluster;
    }

    public static class ClusterController implements CleanCloseable {
        public List<ClusterNode> nodes;

        public JsonNode executeJsScript(String nodeTag, String script) {
            ClusterNode targetNode = getNodeByTag(nodeTag);

            try (IDocumentStore store = new DocumentStore(targetNode.url, null)) {
                store.getConventions().setDisableTopologyUpdates(true);
                store.initialize();

                return store.maintenance().server().send(new AdminJsConsoleOperation(script));
            }
        }

        public JsonNode executeJsScriptRaw(String nodeTag, String script) throws Exception {
            ClusterNode targetNode = getNodeByTag(nodeTag);

            AdminJsConsoleOperation jsConsole = new AdminJsConsoleOperation(script);
            RavenCommand<JsonNode> command = jsConsole.getCommand(new DocumentConventions());

            Reference<String> urlRef = new Reference<>();
            ServerNode serverNode = new ServerNode();
            serverNode.setUrl(targetNode.getUrl());
            HttpRequestBase request = command.createRequest(serverNode, urlRef);
            request.setURI(new URI(urlRef.value));

            try (DocumentStore store = new DocumentStore(targetNode.url, "_")) {
                store.initialize();

                CloseableHttpClient httpClient = store.getRequestExecutor().getHttpClient();

                CloseableHttpResponse response = command.send(httpClient, request);

                if (response.getEntity() != null) {
                    return store.getConventions().getEntityMapper().readTree(response.getEntity().getContent());
                }

                return null;
            }
        }

        public ClusterNode getNodeByUrl(String url) {
            return nodes
                    .stream()
                    .filter(x -> url.equals(x.url))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find node with url: " + url));
        }

        public ClusterNode getWorkingServer() {
            return nodes
                    .stream()
                    .filter(x -> !x.disposed)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find working server"));
        }

        public ClusterNode getNodeByTag(String nodeTag) {
            return nodes
                    .stream()
                    .filter(x -> nodeTag.equals(x.nodeTag))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find node with tag: " + nodeTag));
        }

        public String getCurrentLeader(IDocumentStore store) {
            GetClusterTopologyCommand command = new GetClusterTopologyCommand();
            store.getRequestExecutor().execute(command);

            return command.getResult().getLeader();
        }

        public void disposeServer(String nodeTag) {
            try {
                getNodeByTag(nodeTag).setDisposed(true);
                executeJsScriptRaw(nodeTag, "server.Dispose()");
            } catch (Exception e) {
                // we likely throw as server won't be able to respond
            }
        }

        public ClusterNode getInitialLeader() {
            return nodes
                    .stream()
                    .filter(x -> x.leader)
                    .findFirst()
                    .orElse(null);
        }

        public void createDatabase(String databaseName, int replicationFactor, String leaderUrl) {
            createDatabase(new DatabaseRecord(databaseName), replicationFactor, leaderUrl);
        }

        public DatabasePutResult createDatabase(DatabaseRecord databaseRecord, int replicationFactor, String leaderUrl) {
            try (IDocumentStore store = new DocumentStore(leaderUrl, databaseRecord.getDatabaseName())) {
                store.initialize();

                DatabasePutResult putResult = store.maintenance().server().send(new CreateDatabaseOperation(databaseRecord, replicationFactor));

                for (ClusterNode node : nodes) {
                    executeJsScript(node.nodeTag, "server.ServerStore.Cluster.WaitForIndexNotification(\"" + putResult.getRaftCommandIndex() + "\").Wait()");
                }

                return putResult;
            }
        }

        @Override
        public void close() {
            for (ClusterNode node : nodes) {
                try {
                    node.serverProcess.destroyForcibly();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    protected <T> boolean waitForDocumentInCluster(Class<T> clazz, DocumentSession session, String docId,
                                                   Function<T, Boolean> predicate, Duration timeout) {
        List<ServerNode> nodes = session.getRequestExecutor().getTopologyNodes();
        List<DocumentStore> stores = getDocumentStores(nodes, true);

        return waitForDocumentInClusterInternal(clazz, docId, predicate, timeout, stores);
    }

    private <T> boolean waitForDocumentInClusterInternal(Class<T> clazz, String docId, Function<T, Boolean> predicate,
                                                         Duration timeout, List<DocumentStore> stores) {
        //tasks.Add(Task.Run(() => WaitForDocument(store, docId, predicate, (int)timeout.TotalMilliseconds)));
        for (DocumentStore store : stores) {
            waitForDocument(clazz, store, docId, predicate, timeout.toMillis());
        }

        return true;
    }

    protected <T> boolean waitForDocument(Class<T> clazz, IDocumentStore store, String docId) {
        return waitForDocument(clazz, store, docId, null, 10_000);
    }

    protected <T> boolean waitForDocument(Class<T> clazz, IDocumentStore store, String docId,
                                          Function<T, Boolean> predicate, long timeout) {
        Stopwatch sw = Stopwatch.createStarted();
        Exception ex = null;
        while (sw.elapsed().toMillis() < timeout) {
            try (IDocumentSession session = store.openSession(store.getDatabase())) {
                try {
                    T doc = session.load(clazz, docId);
                    if (doc != null) {
                        if (predicate == null || predicate.apply(doc)) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    ex = e;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // empty
            }
        }
        return false;
    }

    private List<DocumentStore> getDocumentStores(List<ServerNode> nodes, boolean disableTopologyUpdates) {
        List<DocumentStore> stores = new ArrayList<>();
        for (ServerNode node : nodes) {
            DocumentStore store = new DocumentStore(node.getUrl(), node.getDatabase());
            store.getConventions().setDisableTopologyUpdates(disableTopologyUpdates);

            store.initialize();
            stores.add(store);

            _toDispose.add(store);
        }

        return stores;
    }

    @Override
    public void close() {
        for (Closeable closeable : _toDispose) {
            IOUtils.closeQuietly(closeable, null);
        }
    }


    public static class ClusterNode {
        private String nodeTag;
        private String url;
        private boolean leader;
        private IDocumentStore store;
        private Process serverProcess;
        private boolean disposed;

        public boolean isDisposed() {
            return disposed;
        }

        public void setDisposed(boolean disposed) {
            this.disposed = disposed;
        }

        public String getNodeTag() {
            return nodeTag;
        }

        public void setNodeTag(String nodeTag) {
            this.nodeTag = nodeTag;
        }

        public Process getServerProcess() {
            return serverProcess;
        }

        public void setServerProcess(Process serverProcess) {
            this.serverProcess = serverProcess;
        }

        public IDocumentStore getStore() {
            return store;
        }

        public void setStore(IDocumentStore store) {
            this.store = store;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isLeader() {
            return leader;
        }

        public void setLeader(boolean leader) {
            this.leader = leader;
        }
    }
}


