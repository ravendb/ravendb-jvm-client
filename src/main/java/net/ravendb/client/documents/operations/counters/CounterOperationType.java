package net.ravendb.client.documents.operations.counters;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CounterOperationType {
    NONE,
    INCREMENT,
    DELETE,
    GET,
    PUT
}
