package net.ravendb.abstractions.indexing;

import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
public enum TransformerLockMode {
    UNLOCK,
    LOCKED_IGNORE;
}
