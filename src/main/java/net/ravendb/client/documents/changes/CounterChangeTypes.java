package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CounterChangeTypes {
    NONE,
    PUT,
    DELETE,
    INCREMENT
}
