package net.ravendb.client.documents.operations.etl.queue;

import com.fasterxml.jackson.annotation.JsonCreator;
import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum QueueBrokerType {
    NONE,
    KAFKA,
    RABBIT_MQ;

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
            default:
                throw new IllegalArgumentException("Unknown QueueBrokerType: " + value);
        }
    }
}
