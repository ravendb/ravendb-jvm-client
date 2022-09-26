package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum TrafficWatchChangeType {
    NONE,
    QUERIES,
    OPERATIONS,
    MULTI_GET,
    BULK_DOCS,
    INDEX,
    COUNTERS,
    HILO,
    SUBSCRIPTIONS,
    STREAMS,
    DOCUMENTS,
    TIME_SERIES
}
