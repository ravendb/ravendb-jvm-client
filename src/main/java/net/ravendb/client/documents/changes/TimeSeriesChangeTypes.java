package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum TimeSeriesChangeTypes {
    NONE,
    PUT,
    DELETE,
    MIXED
}
