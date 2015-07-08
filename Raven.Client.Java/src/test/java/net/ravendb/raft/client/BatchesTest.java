package net.ravendb.raft.client;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.commands.DeleteCommandData;
import net.ravendb.abstractions.commands.ICommandData;
import net.ravendb.abstractions.commands.PutCommandData;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class BatchesTest extends RaftTestBase {

    private int numberOfNodes;

    public BatchesTest(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    @Test
    public void batchCommandsShouldWork() throws IOException {

        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        DocumentStore store1 = clusterStores.get(0);

        PutCommandData put1 = new PutCommandData("keys/1", null, new RavenJObject(), new RavenJObject());
        PutCommandData put2 = new PutCommandData("keys/2", null, new RavenJObject(), new RavenJObject());

        store1.getDatabaseCommands().batch(Arrays. <ICommandData> asList(put1, put2));

        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/1");
            waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/2");
        }

        store1.getDatabaseCommands().batch(Arrays.<ICommandData> asList(new DeleteCommandData("keys/2", null)));

        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/1");
            waitForDelete(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), "keys/2");
        }
    }
}
