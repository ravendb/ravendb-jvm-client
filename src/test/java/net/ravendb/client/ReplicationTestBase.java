package net.ravendb.client;

import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.replication.ReplicationNode;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.serverwide.ExternalReplication;
import net.ravendb.client.serverwide.etl.RavenConnectionString;
import net.ravendb.client.serverwide.operations.UpdateExternalReplicationOperation;
import net.ravendb.client.serverwide.operations.connectionStrings.PutConnectionStringOperation;

import java.util.concurrent.TimeUnit;

public class ReplicationTestBase extends RemoteTestBase {
    protected void modifyReplicationDestination(ReplicationNode replicationNode) {
        // empty by design
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
        }

        return null;
    }
}
