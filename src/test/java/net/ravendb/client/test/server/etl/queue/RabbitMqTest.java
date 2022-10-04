package net.ravendb.client.test.server.etl.queue;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperation;
import net.ravendb.client.documents.operations.etl.AddEtlOperationResult;
import net.ravendb.client.documents.operations.etl.EtlType;
import net.ravendb.client.documents.operations.etl.Transformation;
import net.ravendb.client.documents.operations.etl.queue.*;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskQueueEtlDetails;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RabbitMqTest extends RemoteTestBase {
    @Test
    public void canSetupRabbitMq() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            QueueConnectionString connectionString = new QueueConnectionString();
            connectionString.setName("r1");
            connectionString.setBrokerType(QueueBrokerType.RABBIT_MQ);
            connectionString.setRabbitMqConnectionSettings(new RabbitMqConnectionSettings());
            connectionString.getRabbitMqConnectionSettings().setConnectionString("r_host");

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            QueueEtlConfiguration etlConfiguration = new QueueEtlConfiguration();
            etlConfiguration.setConnectionStringName("r1");
            Transformation transformation = new Transformation();
            transformation.setCollections(Collections.singletonList("Orders"));
            transformation.setScript("var userData = { UserId: id(this), Name: this.Name }; loadToTest(userData)");
            transformation.setName("Script #1");
            etlConfiguration.setBrokerType(QueueBrokerType.RABBIT_MQ);

            etlConfiguration.setTransforms(Collections.singletonList(transformation));

            AddEtlOperationResult etlResult = store.maintenance().send(new AddEtlOperation<>(etlConfiguration));

            OngoingTaskQueueEtlDetails ongoingTask = (OngoingTaskQueueEtlDetails) store.maintenance()
                    .send(new GetOngoingTaskInfoOperation(etlResult.getTaskId(), OngoingTaskType.QUEUE_ETL));

            assertThat(ongoingTask)
                    .isNotNull();
            assertThat(ongoingTask.getTaskType())
                    .isEqualTo(OngoingTaskType.QUEUE_ETL);
            assertThat(ongoingTask.getConfiguration().getEtlType())
                    .isEqualTo(EtlType.QUEUE);
            assertThat(ongoingTask.getConfiguration().getBrokerType())
                    .isEqualTo(QueueBrokerType.RABBIT_MQ);
            assertThat(ongoingTask.getConfiguration().getTransforms())
                    .hasSize(1);

        }
    }
}
