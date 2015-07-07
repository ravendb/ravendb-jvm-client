package net.ravendb.raft;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.DatabaseDocument;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.replication.ReplicationDocument;
import net.ravendb.client.RavenDBAwareTests;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.utils.SpinWait;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public abstract class RaftTestBase {

    @Rule
    public TestName testName = new TestName();


    private final static int PORT_RANGE_START = 9000;

    private List<DocumentStore> storesToDispose = new ArrayList<>();

    protected static CloseableHttpClient client = HttpClients.createDefault();

    private static AtomicInteger numberOfPortRequests = new AtomicInteger(0);

    protected static int getPort() {
        int portRequest = numberOfPortRequests.incrementAndGet();
        return PORT_RANGE_START - (portRequest % 25);
    }

    @Parameterized.Parameters(name = "Cluster with {0} nodes")
    public static Collection<Object[]> data()  {
        return Arrays.asList(new Object[][]{
                {1}, {3}, {5}
        });
    }

    protected void waitForDocument(IDatabaseCommands iDatabaseCommands, final String documentKey) {
        waitFor(iDatabaseCommands, new Function1<IDatabaseCommands, Boolean>() {
            @Override
            public Boolean apply(IDatabaseCommands input) {
                return input.get(documentKey) != null;

            }
        }, 5000L);
    }

    public void waitForDelete(IDatabaseCommands commands, String key) {
        waitForDelete(commands, key, 5 * 60 * 1000L);
    }

    public void waitForDelete(final IDatabaseCommands commands, final String key, Long timeout) {
        boolean done = SpinWait.spinUntil(new Function0<Boolean>() {
            @Override
            public Boolean apply() {
                // we expect to get the doc from the <system> databse
                JsonDocument doc = commands.get(key);
                return doc == null;
            }
        }, timeout);


        if (!done) {
            throw new RuntimeException("waitForDelete failed");
        }
    }

    public void waitFor(final IDatabaseCommands commands, final Function1<IDatabaseCommands, Boolean> action) {
        waitFor(commands, action, 5 * 60 * 1000L);
    }

    public void waitFor(final IDatabaseCommands commands, final Function1<IDatabaseCommands, Boolean> action, Long timeout) {
        assertTrue(SpinWait.spinUntil(new Function0<Boolean>() {
            @Override
            public Boolean apply() {
                return action.apply(commands);
            }
        }, timeout));
    }

    public List<DocumentStore> createRaftCluster(int numberOfNodes) throws IOException {
        return createRaftCluster(numberOfNodes, null, null, getDbName());
    }

    public List<DocumentStore> createRaftCluster(int numberOfNodes, String activeBundles, Action1<DocumentStore> configureStore, String databaseName) throws IOException {
        List<Integer> ports = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            int port = getPort();
            ports.add(port);
            RavenJObject serverDocument = RavenDBAwareTests.getCreateServerDocument(port);
            RavenDBAwareTests.startServer(port, true, serverDocument);
        }

        Random random = new Random();
        int leaderId = random.nextInt(numberOfNodes);

        List<DocumentStore> systemDbsStores = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            DocumentStore documentStore = new DocumentStore("http://localhost:" + ports.get(i) + "/");
            if (configureStore != null) {
                configureStore.apply(documentStore);
            }
            documentStore.initialize();
            systemDbsStores.add(documentStore);
        }

        DocumentStore leaderStore = systemDbsStores.get(leaderId);
        try (HttpJsonRequest request = leaderStore.getDatabaseCommands().createRequest(HttpMethods.POST, "/admin/cluster/create")) {
            NodeConnectionInfo nci = new NodeConnectionInfo();
            nci.setUri(leaderStore.getUrl());
            request.write(RavenJObject.fromObject(nci).toString());
        }

        waitForElectAsLeader(leaderStore);

        List<DocumentStore> alreadyJoinedNodes = new ArrayList<>();
        alreadyJoinedNodes.add(leaderStore);

        for (int i = 0; i < numberOfNodes; i++) {
            DocumentStore store = systemDbsStores.get(i);
            if (i == leaderId) {
                continue;
            }
            try (HttpJsonRequest request = leaderStore.getDatabaseCommands().createRequest(HttpMethods.POST, "/admin/cluster/join")) {
                try (CleanCloseable _ = store.getDatabaseCommands().disableAllCaching()) {
                    NodeConnectionInfo nci = new NodeConnectionInfo();
                    nci.setUri(store.getUrl());
                    request.write(RavenJObject.fromObject(nci).toString());
                }
            }
            alreadyJoinedNodes.add(store);

            for (DocumentStore joinedNode : alreadyJoinedNodes) {
                waitForNodeInCluster(joinedNode, store.getUrl());
            }
        }

        if (databaseName != null) {
            DatabaseDocument databaseDocument = MultiDatabase.createDatabaseDocument(databaseName);
            if (activeBundles != null) {
                databaseDocument.getSettings().put("Raven/ActiveBundles", activeBundles);
            }

            leaderStore.getDatabaseCommands().forSystemDatabase().getGlobalAdmin().createDatabase(databaseDocument);
        }

        for (DocumentStore node : alreadyJoinedNodes) {
            waitForDocument(node.getDatabaseCommands().forSystemDatabase(), "Raven/Databases/" + getDbName());
        }

        List<DocumentStore> stores = new ArrayList<>();
        for (int i = 0; i < numberOfNodes; i++) {
            DocumentStore documentStore = new DocumentStore("http://localhost:" + ports.get(i), getDbName());
            if (configureStore != null) {
                configureStore.apply(documentStore);
            }
            documentStore.initialize();
            stores.add(documentStore);
            storesToDispose.add(documentStore);
        }

        return stores;
    }

    protected void waitForElectAsLeader(DocumentStore store) {
        waitFor(store.getDatabaseCommands(), new Function1<IDatabaseCommands, Boolean>() {
            @Override
            public Boolean apply(IDatabaseCommands commands) {
                try (HttpJsonRequest request = commands.createRequest(HttpMethods.GET, "/cluster/topology")) {
                    RavenJObject response = (RavenJObject) request.readResponseJson();
                    return "Leader".equals(response.value(String.class, "State"));
                }
            }
        });
    }

    protected void waitForNodeInCluster(DocumentStore leaderStore, final String joiningNodeUrl) {
        waitFor(leaderStore.getDatabaseCommands(), new Function1<IDatabaseCommands, Boolean>() {
            @Override
            public Boolean apply(IDatabaseCommands commands) {
                try (HttpJsonRequest request = commands.createRequest(HttpMethods.GET, "/cluster/topology")) {
                    RavenJObject response = (RavenJObject) request.readResponseJson();
                    RavenJArray allVotingNodes = response.value(RavenJArray.class, "AllVotingNodes");

                    for (RavenJToken votingNode : allVotingNodes) {
                        if (joiningNodeUrl.equals(votingNode.value(String.class, "Uri"))) {
                            return true;
                        }
                    }

                    return "Leader".equals(response.value(String.class, "State"));
                }
            }
        }, 10 * 1000L);
    }

/* TODO
		public List<DocumentStore> ExtendRaftCluster(int numberOfExtraNodes, string activeBundles = null, Action<DocumentStore> configureStore = null, [CallerMemberName] string databaseName = null, bool inMemory = true)
		{
			var leader = servers.FirstOrDefault(server => server.Options.ClusterManager.Value.IsLeader());
			Assert.NotNull(leader);

			var nodes = Enumerable.Range(0, numberOfExtraNodes)
				.Select(x => GetNewServer(GetPort(), activeBundles: activeBundles, databaseName: databaseName, runInMemory:inMemory))
				.ToList();

			var allNodesFinishedJoining = new ManualResetEventSlim();
			leader.Options.ClusterManager.Value.Engine.TopologyChanged += command =>
			{
				if (command.Requested.AllNodeNames.All(command.Requested.IsVoter))
				{
					allNodesFinishedJoining.Set();
				}
			};

			for (var i = 0; i < numberOfExtraNodes; i++)
			{
				var n = nodes[i];

				if (n == leader)
					continue;

				Assert.True(leader.Options.ClusterManager.Value.Engine.AddToClusterAsync(new NodeConnectionInfo
				{
					Name = RaftHelper.GetNodeName(n.SystemDatabase.TransactionalStorage.Id),
					Uri = RaftHelper.GetNodeUrl(n.SystemDatabase.Configuration.ServerUrl)
				}).Wait(10000));
				Assert.True(allNodesFinishedJoining.Wait(10000));
				allNodesFinishedJoining.Reset();
			}

			return nodes
				.Select(node => NewRemoteDocumentStore(ravenDbServer: node, activeBundles: activeBundles, configureStore: configureStore, databaseName: databaseName))
				.ToList();
		}

		public void RemoveFromCluster(RavenDbServer serverToRemove)
		{
			var leader = servers.FirstOrDefault(server => server.Options.ClusterManager.Value.IsLeader());
			if (leader == null)
				throw new InvalidOperationException("Leader is currently not present, thus can't remove node from cluster");
			if (leader == serverToRemove)
			{
				leader.Options.ClusterManager.Value.Engine.StepDownAsync().Wait();
			}
			else
			{
				leader.Options.ClusterManager.Value.Engine.RemoveFromClusterAsync(serverToRemove.Options.ClusterManager.Value.Engine.Options.SelfConnection).Wait(10000);
			}
		}

		private void WaitForClusterToBecomeNonStale(IReadOnlyCollection<RavenDbServer> nodes)
		{
			var numberOfNodes = nodes.Count;
			var result = SpinWait.SpinUntil(() => nodes.All(x => x.Options.ClusterManager.Value.Engine.CurrentTopology.AllVotingNodes.Count() == numberOfNodes), TimeSpan.FromSeconds(10));

			if (result == false)
				throw new InvalidOperationException("Cluster is stale.");
		}

		protected void WaitForClusterToBecomeNonStale(int numberOfNodes)
		{
			servers.ForEach(server => Assert.True(SpinWait.SpinUntil(() =>
			{
				var topology = server.Options.ClusterManager.Value.Engine.CurrentTopology;
				return topology.AllVotingNodes.Count() == numberOfNodes;
			}, TimeSpan.FromSeconds(15))));
		}

*/

    protected void setupClusterConfiguration(List<DocumentStore> clusterStores) throws IOException {
        setupClusterConfiguration(clusterStores, true);
    }

    protected void setupClusterConfiguration(final List<DocumentStore> clusterStores, boolean enableReplication) throws IOException {
        DocumentStore clusterStore = clusterStores.get(0);
        HttpPut httpPut = new HttpPut(clusterStore.getUrl() + "/admin/cluster/commands/configuration");

        ClusterConfiguration clusterConfiguration = new ClusterConfiguration();
        clusterConfiguration.setEnableReplication(enableReplication);

        httpPut.setEntity(new StringEntity(RavenJObject.fromObject(clusterConfiguration).toString()));
        try (CloseableHttpResponse response = client.execute(httpPut)) {
            if (response.getStatusLine().getStatusCode() >= 300) {
                String payload = IOUtils.toString(response.getEntity().getContent());
                throw new RuntimeException(response.getStatusLine().toString() + System.lineSeparator() +  payload);
            }
        }

        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands().forSystemDatabase(), Constants.Global.REPLICATION_DESTINATIONS_DOCUMENT_NAME);
            waitFor(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), new Function1<IDatabaseCommands, Boolean>() {
                @Override
                public Boolean apply(IDatabaseCommands commands) {

                    try (HttpJsonRequest request = commands.createRequest(HttpMethods.GET, "/configuration/replication")) {
                        RavenJObject replicationDocumentation = (RavenJObject)request.readResponseJson();
                        if (replicationDocumentation == null) {
                            return false;
                        }

                        ReplicationDocument replicatonDocument = JsonExtensions.createDefaultJsonSerializer().readValue(replicationDocumentation.toString(), ReplicationDocument.class);
                        return replicatonDocument.getDestinations().size() == clusterStores.size() - 1;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    @After
    public void cleanup() {
        for (DocumentStore documentStore : storesToDispose) {
            try {
                URL url = new URL(documentStore.getUrl());
                int port = url.getPort();
                RavenDBAwareTests.stopServer(port);
            } catch (Exception e) {
                //ignore
            }
        }
        storesToDispose.clear();
    }

    protected String getDbName() {
        String method = testName.getMethodName();
        return RavenDBAwareTests.getDbNameBasedOnTestName(method);
    }
}
