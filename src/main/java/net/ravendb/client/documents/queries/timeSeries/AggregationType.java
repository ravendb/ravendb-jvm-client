package net.ravendb.client.documents.queries.timeSeries;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum AggregationType {

    // The order here matters.
    // When executing an aggregation query over rolled-up series,
    // we take just the appropriate aggregated value from each entry,
    // according to the aggregation's position in this enum (e.g. AggregationType.Min => take entry.Values[2])

    FIRST,
    LAST,
    MIN,
    MAX,
    SUM,
    COUNT,
    AVERAGE
}
