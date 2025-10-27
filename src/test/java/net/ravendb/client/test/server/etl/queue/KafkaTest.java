package net.ravendb.client.test.server.etl.queue;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperationResult;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.operations.etl.queue.KafkaConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueEtlConfiguration;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskQueueEtl;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class KafkaTest extends RemoteTestBase {
    @Test
    public void canSetupKafka() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            QueueConnectionString connectionString = new QueueConnectionString();
            connectionString.setName("k1");
            connectionString.setBrokerType(QueueBrokerType.KAFKA);
            connectionString.setKafkaConnectionSettings(new KafkaConnectionSettings());
            connectionString.getKafkaConnectionSettings().setBootstrapServers("localhost:9092");

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            QueueEtlConfiguration etlConfiguration = new QueueEtlConfiguration();
            etlConfiguration.setConnectionStringName("k1");
            etlConfiguration.setBrokerType(QueueBrokerType.KAFKA);
            Transformation transformation = new Transformation();
            transformation.setCollections(Collections.singletonList("Orders"));
            transformation.setScript("var userData = { UserId: id(this), Name: this.Name }; loadToTest(userData)");
            transformation.setName("Script #1");

            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperationResult etlResult = store.maintenance().send(new AddEtlOperation<>(etlConfiguration));

            OngoingTaskQueueEtl ongoingTask = (OngoingTaskQueueEtl) store.maintenance()
                    .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.QUEUE_ETL));

            assertThat(ongoingTask)
                    .isNotNull();
            assertThat(ongoingTask.getTaskType())
                    .isEqualTo(OngoingTaskType.QUEUE_ETL);
            assertThat(ongoingTask.getConfiguration().getEtlType())
                    .isEqualTo(EtlType.QUEUE);
            assertThat(ongoingTask.getConfiguration().getBrokerType())
                    .isEqualTo(QueueBrokerType.KAFKA);
            assertThat(ongoingTask.getConfiguration().getTransforms())
                    .hasSize(1);

        }
    }
}
