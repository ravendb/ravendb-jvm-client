package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.queueSink.QueueSinkConfiguration;

public class OngoingTaskQueueSink extends OngoingTask {

    private QueueBrokerType brokerType;
    private String connectionStringName;
    private String url;

    private QueueSinkConfiguration configuration;

    public OngoingTaskQueueSink() {
        setTaskType(OngoingTaskType.QUEUE_SINK);
    }

    public QueueBrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(QueueBrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public QueueSinkConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(QueueSinkConfiguration configuration) {
        this.configuration = configuration;
    }
}
