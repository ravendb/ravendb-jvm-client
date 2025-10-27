package net.ravendb.client.test.server.etl.sink;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.documents.operations.connectionStrings.PutConnectionStringOperation;
import net.ravendb.client.documents.operations.etl.queue.KafkaConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.queue.RabbitMqConnectionSettings;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskQueueSink;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskState;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.operations.queueSink.*;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class QueueSinkTest extends RemoteTestBase {
    @Test
    public void canSetupKafkaSink() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            QueueConnectionString connectionString = new QueueConnectionString();
            connectionString.setName("k1");
            connectionString.setBrokerType(QueueBrokerType.KAFKA);
            connectionString.setKafkaConnectionSettings(new KafkaConnectionSettings());
            connectionString.getKafkaConnectionSettings().setBootstrapServers("localhost:9092");

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            setupQueueSink(QueueBrokerType.KAFKA, store, connectionString.getName());
        }
    }

    @Test
    public void canSetupRabbitMqSink() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            QueueConnectionString connectionString = new QueueConnectionString();
            connectionString.setName("k1");
            connectionString.setBrokerType(QueueBrokerType.RABBIT_MQ);
            RabbitMqConnectionSettings rabbitMqConnectionSettings = new RabbitMqConnectionSettings();
            rabbitMqConnectionSettings.setConnectionString("localhost:9050");
            connectionString.setRabbitMqConnectionSettings(rabbitMqConnectionSettings);

            store.maintenance().send(new PutConnectionStringOperation<>(connectionString));

            setupQueueSink(QueueBrokerType.RABBIT_MQ, store, connectionString.getName());
        }
    }

    private void setupQueueSink(QueueBrokerType brokerType, DocumentStore store, String connectionStringName) {
        QueueSinkConfiguration queueSinkConfiguration = new QueueSinkConfiguration();
        queueSinkConfiguration.setBrokerType(brokerType);
        queueSinkConfiguration.setDisabled(true);
        queueSinkConfiguration.setName("QueueSink");
        queueSinkConfiguration.setConnectionStringName(connectionStringName);

        QueueSinkScript script = new QueueSinkScript();
        script.setQueues(Collections.singletonList("users"));
        script.setScript("this.a = 5");
        script.setName("Script #1");
        queueSinkConfiguration.setScripts(Collections.singletonList(script));

        AddQueueSinkOperationResult addResult = store.maintenance().send(new AddQueueSinkOperation<>(queueSinkConfiguration));

        assertThat(addResult)
                .isNotNull();
        long taskId = addResult.getTaskId();

        OngoingTaskQueueSink sink = (OngoingTaskQueueSink) store.maintenance().send(
                new GetOngoingTaskInfoOperation(taskId, OngoingTaskType.QUEUE_SINK));
        assertThat(sink.getBrokerType())
                .isEqualTo(brokerType);
        assertThat(sink.getConnectionStringName())
                .isEqualTo(connectionStringName);
        assertThat(sink.getTaskState())
                .isEqualTo(OngoingTaskState.DISABLED);

        sink.getConfiguration().setDisabled(false);

        UpdateQueueSinkOperationResult updateResult = store.maintenance().send(new UpdateQueueSinkOperation<>(taskId, sink.getConfiguration()));
        assertThat(updateResult)
                .isNotNull();
    }
}
