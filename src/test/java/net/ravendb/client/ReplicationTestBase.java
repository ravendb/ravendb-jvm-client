package net.ravendb.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.UpdateExternalReplicationOperation;
import net.ravendb.client.documents.replication.ReplicationNode;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import org.assertj.core.api.Assertions;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SameParameterValue")
public class ReplicationTestBase extends RemoteTestBase {
    @SuppressWarnings("EmptyMethod")
    protected void modifyReplicationDestination(ReplicationNode replicationNode) {
        // empty by design
    }

    public static class Marker {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    protected void ensureReplicating(IDocumentStore src, IDocumentStore dst) {
        String id = "marker/" + UUID.randomUUID();

        try (IDocumentSession s = src.openSession()) {
            s.store(new Marker(), id);
            s.saveChanges();
        }

        assertThat(waitForDocumentToReplicate(dst, ObjectNode.class, id, 15_000))
            .isNotNull();
    }

    protected void setupReplication(IDocumentStore fromStore, IDocumentStore... destinations) {

        for (IDocumentStore store : destinations) {
            ExternalReplication databaseWatcher = new ExternalReplication(store.getDatabase(), "ConnectionString-" + store.getIdentifier());
            modifyReplicationDestination(databaseWatcher);

            addWatcherToReplicationTopology(fromStore, databaseWatcher);
        }
    }

    private void addWatcherToReplicationTopology(IDocumentStore store, ExternalReplication watcher) {

        RavenConnectionString connectionString = new RavenConnectionString();
        connectionString.setName(watcher.getConnectionStringName());
        connectionString.setDatabase(watcher.getDatabase());
        connectionString.setTopologyDiscoveryUrls(store.getUrls());

        store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

        UpdateExternalReplicationOperation op = new UpdateExternalReplicationOperation(watcher);
        store.maintenance().send(op);
    }

    protected <T> T waitForDocumentToReplicate(IDocumentStore store, Class<T> clazz, String id, int timeout) {
        Stopwatch sw = Stopwatch.createStarted();

        while (sw.elapsed(TimeUnit.MILLISECONDS) <= timeout) {
            try (IDocumentSession session = store.openSession()) {
                T doc = session.load(clazz, id);
                if (doc != null) {
                    return doc;
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }

        return null;
    }
}
