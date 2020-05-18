package net.ravendb.client.documents.indexes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum IndexSourceType {
    NONE,
    DOCUMENTS,
    TIME_SERIES,
    COUNTERS
}
