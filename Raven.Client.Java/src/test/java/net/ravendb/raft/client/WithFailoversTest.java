package net.ravendb.raft.client;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class WithFailoversTest extends RaftTestBase {

    @Test
    public void readFromLeaderWriteToLeaderWithFailoverShouldWork() throws IOException {
        withFailoversInternal(3, ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER_WITH_FAILOVERS);
    }

    @Test
    public void readFromAllWriteToLeaderWithFailoversShouldWork() throws IOException {
        withFailoversInternal(3, ClusterBehavior.READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS);
    }

    private void withFailoversInternal(int numberOfNodes, final ClusterBehavior clusterBehavior) throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(clusterBehavior);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (DocumentStore store : clusterStores) {
            ((ServerClient)store.getDatabaseCommands()).getRequestExecuter().updateReplicationInformationIfNeeded((ServerClient) store.getDatabaseCommands(), true);
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().put("keys/" + i, null, new RavenJObject(), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore clusterStore : clusterStores) {
                waitForDocument(clusterStore.getDatabaseCommands(), "keys/" + i);
            }
        }

        stopLeader();

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().put("keys/" + ( i + clusterStores.size()), null, new RavenJObject(), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            for (DocumentStore store : clusterStores) {
                waitForDocument(store.getDatabaseCommands(), "keys/" + (i + clusterStores.size()));
            }
        }
    }
}
