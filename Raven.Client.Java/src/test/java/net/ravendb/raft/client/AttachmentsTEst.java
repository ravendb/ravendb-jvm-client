package net.ravendb.raft.client;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@RunWith(Parameterized.class)
public class AttachmentsTest extends RaftTestBase {

    private int numberOfNodes;

    public AttachmentsTest(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    @Test
    public void putShouldBePropaged() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().putAttachment("keys/" + i, null, new ByteArrayInputStream(new byte[0]), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            final int iCopy = i;
            for (DocumentStore store : clusterStores) {
                waitFor(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), new Function1<IDatabaseCommands, Boolean>() {
                    @Override
                    public Boolean apply(IDatabaseCommands commands) {
                        return commands.getAttachment("keys/" + iCopy) != null;
                    }
                });
            }
        }
    }

    @Test
    public void deleteShouldBePropagated() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(numberOfNodes, "Replication", new Action1<DocumentStore>() {
            @Override
            public void apply(DocumentStore store) {
                store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            }
        }, getDbName());

        setupClusterConfiguration(clusterStores);

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().putAttachment("keys/" + i, null, new ByteArrayInputStream(new byte[0]), new RavenJObject());
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            final int iCopy = i;
            for (DocumentStore store : clusterStores) {
                waitFor(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), new Function1<IDatabaseCommands, Boolean>() {
                    @Override
                    public Boolean apply(IDatabaseCommands commands) {
                        return commands.getAttachment("keys/" + iCopy) != null;
                    }
                });
            }
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            DocumentStore store = clusterStores.get(i);
            store.getDatabaseCommands().deleteAttachment("keys/" + i, null);
        }

        for (int i = 0; i < clusterStores.size(); i++) {
            final int iCopy = i;
            for (DocumentStore store : clusterStores) {
                waitFor(store.getDatabaseCommands().forDatabase(store.getDefaultDatabase(), ClusterBehavior.NONE), new Function1<IDatabaseCommands, Boolean>() {
                    @Override
                    public Boolean apply(IDatabaseCommands commands) {
                        return commands.getAttachment("keys/" + iCopy) == null;
                    }
                });
            }
        }
    }
}
