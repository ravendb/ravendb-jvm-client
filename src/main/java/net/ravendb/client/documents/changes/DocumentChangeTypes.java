package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum DocumentChangeTypes {
    NONE,

    PUT,
    DELETE,
    CONFLICT,
    COMMON
}
