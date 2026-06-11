package net.ravendb.client.documents.session;

/**
 * Controls where {@code null} values are placed in the result of an {@code ORDER BY} clause.
 * <p>
 * Per-query null placement ({@link #FIRST} / {@link #LAST}) is supported only by the Corax indexing engine.
 * Queries that specify {@link #FIRST} or {@link #LAST} against a Lucene index are rejected.
 */
public enum NullsOrdering {
    /**
     * No per-query placement is specified; the index/server configuration decides where nulls go.
     */
    DEFAULT,

    /**
     * Null values appear first in the result, regardless of sort direction.
     * Supported only by the Corax indexing engine.
     */
    FIRST,

    /**
     * Null values appear last in the result, regardless of sort direction.
     * Supported only by the Corax indexing engine.
     */
    LAST
}
