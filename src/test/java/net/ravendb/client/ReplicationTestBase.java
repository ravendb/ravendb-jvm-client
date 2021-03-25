package net.ravendb.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.ongoingTasks.DeleteOngoingTaskOperation;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.operations.replication.*;
import net.ravendb.client.documents.replication.ReplicationNode;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;

import java.util.ArrayList;
import java.util.List;
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

    protected List<ModifyOngoingTaskResult> setupReplication(IDocumentStore fromStore, IDocumentStore... destinations) {
        List<ModifyOngoingTaskResult> result = new ArrayList<>();

        for (IDocumentStore store : destinations) {
            ExternalReplication databaseWatcher = new ExternalReplication(store.getDatabase(), "ConnectionString-" + store.getIdentifier());
            modifyReplicationDestination(databaseWatcher);

            result.add(addWatcherToReplicationTopology(fromStore, databaseWatcher));
        }

        return result;
    }

    protected ModifyOngoingTaskResult addWatcherToReplicationTopology(IDocumentStore store, ExternalReplicationBase watcher, String... urls) {

        RavenConnectionString connectionString = new RavenConnectionString();
        connectionString.setName(watcher.getConnectionStringName());
        connectionString.setDatabase(watcher.getDatabase());

        String[] urlsToUse = urls != null && urls.length > 0 ? urls : store.getUrls();

        connectionString.setTopologyDiscoveryUrls(urlsToUse);

        store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

        IMaintenanceOperation<ModifyOngoingTaskResult> op;

        if (watcher instanceof PullReplicationAsSink) {
            op = new UpdatePullReplicationAsSinkOperation((PullReplicationAsSink) watcher);
        } else if (watcher instanceof ExternalReplication) {
            op = new UpdateExternalReplicationOperation((ExternalReplication) watcher);
        } else {
            throw new IllegalArgumentException("Unrecognized type: " + watcher.getClass());
        }

        return store.maintenance().send(op);
    }

    protected static ModifyOngoingTaskResult deleteOngoingTask(DocumentStore store, long taskId, OngoingTaskType taskType) {
        DeleteOngoingTaskOperation op = new DeleteOngoingTaskOperation(taskId, taskType);
        return store.maintenance().send(op);
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
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        return null;
    }
}
