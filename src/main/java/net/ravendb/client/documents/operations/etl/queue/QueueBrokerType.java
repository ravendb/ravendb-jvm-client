package net.ravendb.client.documents.operations.etl.queue;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum QueueBrokerType {
    NONE,
    KAFKA,
    RABBIT_MQ
}
