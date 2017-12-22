package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum IndexLockMode {
    UNLOCK,
    LOCKED_IGNORE,
    LOCKED_ERROR
}
