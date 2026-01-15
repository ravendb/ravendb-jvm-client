package net.ravendb.client.documents.operations.etl.queue;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QueueEtlConfiguration extends EtlConfiguration<QueueConnectionString> {

    public QueueEtlConfiguration() {
        this.queues = new ArrayList<EtlQueue>();
    }

    @Override
    public String getDestination() {
        return this.getConnection().getUrl();
    }

    @Override
    public String getDefaultTaskName() {
        return "Queue ETL to " + this.getConnectionStringName();
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {

        switch (brokerType) {
            case KAFKA:
                String protocol = this.getConnection().getKafkaConnectionSettings()
                        .getConnectionOptions()
                        .get("security.protocol");

                if (protocol != null) {
                    return protocol.toLowerCase(Locale.ROOT).contains("ssl");
                }
                break;
            case RABBIT_MQ:
                return this.getConnection().getRabbitMqConnectionSettings()
                        .getConnectionString()
                        .regionMatches(true, 0, "amqps", 0, "amqps".length());
            case AZURE_QUEUE_STORAGE:
                return this.getConnection().getAzureQueueStorageConnectionSettings()
                        .getStorageUrl()
                        .regionMatches(true, 0, "https", 0, "https".length());
            case AMAZON_SQS:
                return this.getConnection().getAmazonSqsConnectionSettings()
                        .getQueueUrl()
                        .regionMatches(true, 0, "https", 0, "https".length());
            default:
                throw new UnsupportedOperationException(
                        "Unknown broker type: " + brokerType
                );
        }
        return false;
    }

    @Override
    public EtlType getEtlType() {
        return EtlType.QUEUE;
    }

    private List<EtlQueue> queues;

    private QueueBrokerType brokerType;

    private boolean skipAutomaticQueueDeclaration;

    public List<EtlQueue> getQueues() {
        return queues;
    }

    public void setQueues(List<EtlQueue> queues) {
        this.queues = queues;
    }

    public QueueBrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(QueueBrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public boolean isSkipAutomaticQueueDeclaration() {
        return skipAutomaticQueueDeclaration;
    }

    public void setSkipAutomaticQueueDeclaration(boolean skipAutomaticQueueDeclaration) {
        this.skipAutomaticQueueDeclaration = skipAutomaticQueueDeclaration;
    }

}
