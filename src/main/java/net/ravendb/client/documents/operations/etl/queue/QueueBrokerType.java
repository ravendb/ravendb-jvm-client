package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum QueueBrokerType {
    NONE,
    KAFKA,
    RABBIT_MQ,
    AZURE_QUEUE_STORAGE,
    AMAZON_SQS,
    AZURE_SERVICE_BUS;

    @JsonCreator
    public static QueueBrokerType fromString(String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case "KAFKA":
                return KAFKA;
            case "Kafka":
                return KAFKA;
            case "RABBIT_MQ":
                return RABBIT_MQ;
            case "RabbitMq":
                return RABBIT_MQ;
            case "NONE":
                return NONE;
            case "None":
                return NONE;
            case "AZURE_QUEUE_STORAGE":
                return AZURE_QUEUE_STORAGE;
            case "AzureQueueStorage":
                return AZURE_QUEUE_STORAGE;
            case "AMAZON_SQS":
                return AMAZON_SQS;
            case "AmazonSqs":
                return AMAZON_SQS;
            case "AZURE_SERVICE_BUS":
                return AZURE_SERVICE_BUS;
            case "AzureServiceBus":
                return AZURE_SERVICE_BUS;
            default:
                throw new IllegalArgumentException("Unknown QueueBrokerType: " + value);
        }
    }
}
