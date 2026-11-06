package net.ravendb.client.test.issues;

import net.ravendb.client.ReplicationTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskReplication;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_22955Test extends ReplicationTestBase {

    @Test
    public void ongoingTaskReplicationContainsReplicationProgress() throws Exception {
        try (IDocumentStore source = getDocumentStore();
             IDocumentStore destination = getDocumentStore()) {

            List<ModifyOngoingTaskResult> tasks = setupReplication(source, destination);
            ensureReplicating(source, destination);

            long taskId = tasks.get(0).getTaskId();

            // RavenDB-21922 & RavenDB-22955: replication progress is exposed on the ongoing task info
            waitForValue(() -> {
                OngoingTaskReplication task = (OngoingTaskReplication) source.maintenance()
                        .send(new GetOngoingTaskInfoOperation(taskId, OngoingTaskType.REPLICATION));
                return task.getLastSentEtag() > 0 && task.getLastSentEtag() == task.getLastDatabaseEtag();
            }, true);

            OngoingTaskReplication task = (OngoingTaskReplication) source.maintenance()
                    .send(new GetOngoingTaskInfoOperation(taskId, OngoingTaskType.REPLICATION));

            assertThat(task.getFromToString())
                    .isNotNull()
                    .contains(source.getDatabase());
            assertThat(task.getSourceDatabaseChangeVector())
                    .isNotNull();
            assertThat(task.getLastAcceptedChangeVectorFromDestination())
                    .isNotNull();
            assertThat(task.getLastSentEtag())
                    .isPositive();
            assertThat(task.getLastDatabaseEtag())
                    .isEqualTo(task.getLastSentEtag());
        }
    }
}
