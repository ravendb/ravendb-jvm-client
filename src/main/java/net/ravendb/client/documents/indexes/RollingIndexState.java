package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum RollingIndexState {
    PENDING,
    RUNNING,
    DONE
}
