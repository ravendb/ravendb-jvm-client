package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueEtlConfiguration;

public class OngoingTaskQueueEtl extends OngoingTask {
    private QueueBrokerType brokerType;
    private String connectionStringName;
    private String url;
    private QueueEtlConfiguration configuration;

    public OngoingTaskQueueEtl() {
        setTaskType(OngoingTaskType.QUEUE_ETL);
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

    public QueueEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(QueueEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
