package net.ravendb.client.documents.queries;

/**
 * Defines type of aggregation for a field.
 */
public enum GroupByMethod {
    /**
     * Each value from field will be treated separately.
     */
    NONE,
    /**
     * Whole array is treated as single value (hash is calculated from whole array).
     */
    ARRAY
}
