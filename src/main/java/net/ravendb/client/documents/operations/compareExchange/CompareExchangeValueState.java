package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CompareExchangeValueState {
    NONE,
    CREATED,
    DELETED,
    MISSING
}
