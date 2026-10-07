package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueConnectionString extends ConnectionString {

    private QueueBrokerType brokerType;
    private KafkaConnectionSettings kafkaConnectionSettings;
    private RabbitMqConnectionSettings rabbitMqConnectionSettings;
    private AzureQueueStorageConnectionSettings azureQueueStorageConnectionSettings;
    private AmazonSqsConnectionSettings amazonSqsConnectionSettings;
    private AzureServiceBusConnectionSettings azureServiceBusConnectionSettings;

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.QUEUE;
    }

    public QueueBrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(QueueBrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public KafkaConnectionSettings getKafkaConnectionSettings() {
        return kafkaConnectionSettings;
    }

    public void setKafkaConnectionSettings(KafkaConnectionSettings kafkaConnectionSettings) {
        this.kafkaConnectionSettings = kafkaConnectionSettings;
    }

    public RabbitMqConnectionSettings getRabbitMqConnectionSettings() {
        return rabbitMqConnectionSettings;
    }

    public void setRabbitMqConnectionSettings(RabbitMqConnectionSettings rabbitMqConnectionSettings) {
        this.rabbitMqConnectionSettings = rabbitMqConnectionSettings;
    }

    public void setAzureQueueStorageConnectionSettings(AzureQueueStorageConnectionSettings azureQueueStorageConnectionSettings) { this.azureQueueStorageConnectionSettings = azureQueueStorageConnectionSettings; }

    public AzureQueueStorageConnectionSettings getAzureQueueStorageConnectionSettings() {
        return azureQueueStorageConnectionSettings;
    }

    public void setAmazonSqsConnectionSettings(AmazonSqsConnectionSettings amazonSqsConnectionSettings) { this.amazonSqsConnectionSettings = amazonSqsConnectionSettings; }

    public AmazonSqsConnectionSettings getAmazonSqsConnectionSettings() { return this.amazonSqsConnectionSettings; }

    public void setAzureServiceBusConnectionSettings(AzureServiceBusConnectionSettings azureServiceBusConnectionSettings) { this.azureServiceBusConnectionSettings = azureServiceBusConnectionSettings; }

    public AzureServiceBusConnectionSettings getAzureServiceBusConnectionSettings() { return this.azureServiceBusConnectionSettings; }
}
