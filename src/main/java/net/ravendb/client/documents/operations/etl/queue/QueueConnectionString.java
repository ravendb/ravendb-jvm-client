package net.ravendb.client.documents.operations.etl.queue;

import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

public class QueueConnectionString extends ConnectionString {

    private QueueBrokerType brokerType;

    private KafkaConnectionSettings kafkaConnectionSettings;
    private RabbitMqConnectionSettings rabbitMqConnectionSettings;

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
}
