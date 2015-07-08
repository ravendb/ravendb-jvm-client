package net.ravendb.raft.client;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.request.ClusterAwareRequestExecuter;
import net.ravendb.client.connection.request.ReplicationAwareRequestExecuter;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;


public class BasicTest extends RaftTestBase {


    @Test
    public void requestExecuterShouldDependOnClusterBehavior() {
        try (DocumentStore store = new DocumentStore("http://localhost:9000")) {
            store.initialize();

            assertEquals(ClusterBehavior.NONE, store.getConventions().getClusterBehavior());

            ServerClient client = (ServerClient) store.getDatabaseCommands();
            assertTrue(client.getRequestExecuter() instanceof ReplicationAwareRequestExecuter);

            client = (ServerClient) store.getDatabaseCommands().forSystemDatabase();
            assertTrue(client.getRequestExecuter() instanceof ReplicationAwareRequestExecuter);

            ServerClient defaultClient = (ServerClient) store.getDatabaseCommands();
            client = (ServerClient) defaultClient.forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE);
            assertTrue(client.getRequestExecuter() instanceof ReplicationAwareRequestExecuter);
            assertSame(defaultClient, client);

            client = (ServerClient) defaultClient.forDatabase(store.getDefaultDatabase(), ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            assertTrue(client.getRequestExecuter() instanceof ClusterAwareRequestExecuter);
        }
    }

    @RunWith(Parameterized.class)
    public static class WithParamTest extends RaftTestBase {

        private int numberofnodes;

        public WithParamTest(int numberofnodes) {
            this.numberofnodes = numberofnodes;
        }

        @Test
        public void clientsShouldBeAbleToPerformCommandsEvenIfTheyDoNotPointToLeader() throws IOException { //TODO: inline data
            List<DocumentStore> clusterStores = createRaftCluster(numberofnodes, "Replication", new Action1<DocumentStore>() {
                @Override
                public void apply(DocumentStore store) {
                    store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
                }
            }, getDbName());

            setupClusterConfiguration(clusterStores);

            for (int i = 0; i < clusterStores.size(); i++) {
                DocumentStore store = clusterStores.get(i);
                store.getDatabaseCommands().put("keys/" + i, null, new RavenJObject(), new RavenJObject());
            }

            for (int i = 0; i < clusterStores.size(); i++) {
                for (DocumentStore store : clusterStores) {
                    waitForDocument(store.getDatabaseCommands(), "keys/" + i);
                }
            }

            for (int i = 0; i < clusterStores.size(); i++) {
                DocumentStore store = clusterStores.get(i);
                store.getDatabaseCommands().put("keys/" + (i + clusterStores.size()), null, new RavenJObject(), new RavenJObject());
            }

            for (int i = 0; i < clusterStores.size(); i++) {
                for (DocumentStore store : clusterStores) {
                    waitForDocument(store.getDatabaseCommands(), "keys/" + (i + clusterStores.size()));
                }
            }
        }
    }

    @Test
    public void nonClusterCommandsCanPerformCommandsOnClusterServers() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(2, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        try (DocumentStore store1 = clusterStores.get(0);
        DocumentStore store2 = clusterStores.get(1)) {
            ServerClient nonClusterCommands1 = (ServerClient) store1.getDatabaseCommands().forDatabase(store1.getDefaultDatabase(), ClusterBehavior.NONE);
            ServerClient nonClusterCommands2 = (ServerClient) store2.getDatabaseCommands().forDatabase(store2.getDefaultDatabase(), ClusterBehavior.NONE);

            nonClusterCommands1.put("keys/1", null, new RavenJObject(), new RavenJObject());
            nonClusterCommands2.put("keys/2", null, new RavenJObject(), new RavenJObject());

            for (DocumentStore clusterStore : clusterStores) {
                waitForDocument(clusterStore.getDatabaseCommands(), "keys/1");
                waitForDocument(clusterStore.getDatabaseCommands(), "keys/2");
            }
        }
    }

    @Test
    public void clientShouldHandleLeaderShutdown() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(3, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (DocumentStore clusterStore : clusterStores) {
            ((ServerClient)clusterStore.getDatabaseCommands()).getRequestExecuter().updateReplicationInformationIfNeeded((ServerClient) clusterStore.getDatabaseCommands(), true);
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().put("keys/" + i, null, new RavenJObject(), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore store : clusterStores) {
                waitForDocument(store.getDatabaseCommands(), "keys/" + i);
            }
        }

        DocumentStore leaderStore = stopLeader();

        try {
            for (int i = 0; i < clusterStores.size(); i++) {
                DocumentStore store = clusterStores.get(i);
                store.getDatabaseCommands().put("keys/" + (i + clusterStores.size()), null, new RavenJObject(), new RavenJObject());
            }

            for (int i = 0; i < clusterStores.size(); i++) {
                for (DocumentStore store : clusterStores) {
                    waitForDocument(store.getDatabaseCommands(), "keys/" + (i + clusterStores.size()));
                }
            }
        } finally {
            leaderStore.close();
        }
    }
}
