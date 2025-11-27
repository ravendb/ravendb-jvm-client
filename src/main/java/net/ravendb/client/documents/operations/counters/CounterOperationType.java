package net.ravendb.client.documents.operations.counters;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Represents the types of operations that can be performed on counters in RavenDB.
 */
@UseSharpEnum
public enum CounterOperationType {
    /**
     * Represents the absence of any counter operation.
     */
    NONE,
    /**
     * Increases the value of the counter by a specified amount. If the counter does not exist, it will be created with the specified value.
     */
    INCREMENT,
    /**
     * Deletes the counter, removing it from the database.
     */
    DELETE,
    /**
     * Retrieves the value of a specific counter.
     */
    GET,
    /**
     * Used internally for sending counters by ETL
     */
    PUT,
    /**
     * Retrieves all counters associated with a document. When using this type, 'CounterName' should not be specified.
     */
    GET_ALL
}
