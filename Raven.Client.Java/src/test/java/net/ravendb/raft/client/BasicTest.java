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

@RunWith(Parameterized.class)
public class BasicTest extends RaftTestBase {

    private int numberOfNodes;

    public BasicTest(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

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

    @Test
    public void clientsShouldBeAbleToPerformCommandsEvenIfTheyDoNotPointToLeader() throws IOException { //TODO: inline data
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
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

    //TODO:
}
