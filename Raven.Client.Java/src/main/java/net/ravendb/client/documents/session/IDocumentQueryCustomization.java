package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.operations.QueryOperation;

import java.time.Duration;

public interface IDocumentQueryCustomization<T> {

    /**
     * Get the raw query operation that will be sent to the server
     */
    QueryOperation getQueryOperation();

    /**
     * Disables caching for query results.
     */
    T noCaching();

    /**
     * Disables tracking for queried entities by Raven's Unit of Work.
     * Usage of this option will prevent holding query results in memory.
     */
    T noTracking();

    /**
     * Order the search results randomly
     */
    T randomOrdering();

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     */
    T randomOrdering(String seed);

    //TBD IDocumentQueryCustomization CustomSortUsing(string typeName);
    //TBD IDocumentQueryCustomization CustomSortUsing(string typeName, bool descending);
    //TBD IDocumentQueryCustomization ShowTimings();

    /**
     * Instruct the query to wait for non stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     * Maximum time to wait for index query results to become non-stale before exception is thrown. Default: 15 seconds.
     */
    T waitForNonStaleResults(Duration waitTimeout);
}
