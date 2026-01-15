package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

import java.util.List;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueConnectionString extends ConnectionString {

    private QueueBrokerType brokerType;
    private KafkaConnectionSettings kafkaConnectionSettings;
    private RabbitMqConnectionSettings rabbitMqConnectionSettings;
    private AzureQueueStorageConnectionSettings azureQueueStorageConnectionSettings;
    private AmazonSqsConnectionSettings amazonSqsConnectionSettings;

    @Override
    protected void validateImpl(List<String> errors) {

        switch (brokerType) {
            case KAFKA:
                if (kafkaConnectionSettings == null ||
                        kafkaConnectionSettings.getBootstrapServers() == null ||
                        kafkaConnectionSettings.getBootstrapServers().trim().isEmpty()) {

                    errors.add("KafkaConnectionSettings has no valid setting.");
                }
                break;
            case RABBIT_MQ:
                if (rabbitMqConnectionSettings == null ||
                        rabbitMqConnectionSettings.getConnectionString() == null ||
                        rabbitMqConnectionSettings.getConnectionString().trim().isEmpty()) {

                    errors.add("RabbitMqConnectionSettings has no valid setting.");
                }
                break;
            case AZURE_QUEUE_STORAGE:
                if (!azureQueueStorageConnectionSettings.isValidConnection()) {
                    errors.add("AzureQueueStorageConnectionSettings has no valid setting.");
                }
                break;
            case AMAZON_SQS:
                if (!amazonSqsConnectionSettings.isValidConnection()) {
                    errors.add("AmazonSqsConnectionSettings has no valid setting.");
                }
                break;
            default:
                throw new UnsupportedOperationException("'" + brokerType + "' broker is not supported");
        }
    }

    String getUrl() {
        String url;
        switch (brokerType) {

            case KAFKA:
                url = kafkaConnectionSettings.getBootstrapServers();
                break;

            case RABBIT_MQ:
                String connectionString = rabbitMqConnectionSettings.getConnectionString();

                int indexOfStartServerUri =
                        connectionString.toLowerCase(Locale.ROOT).indexOf("@");

                url = (indexOfStartServerUri != -1)
                        ? connectionString.substring(indexOfStartServerUri + 1)
                        : null;
                break;

            case AZURE_QUEUE_STORAGE:
                url = azureQueueStorageConnectionSettings.getStorageUrl();
                break;

            case AMAZON_SQS:
                url = amazonSqsConnectionSettings.getQueueUrl();
                break;

            default:
                throw new UnsupportedOperationException(
                        "'" + brokerType + "' broker is not supported"
                );
        }
        return url;
    }

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
}
