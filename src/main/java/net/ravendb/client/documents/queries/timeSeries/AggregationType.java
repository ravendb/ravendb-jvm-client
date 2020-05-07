package net.ravendb.client.documents.queries.timeSeries;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum AggregationType {
    MIN,
    MAX,
    AVERAGE,
    FIRST,
    LAST,
    SUM,
    COUNT
}
