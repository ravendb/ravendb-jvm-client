package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.function.Consumer;

public interface IQueryBase<T, TSelf extends IQueryBase<T, TSelf>> {

    /**
     * Gets the document convention from the query session
     */
    DocumentConventions getConventions();

    TSelf addBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    TSelf removeBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    TSelf addAfterQueryExecutedListener(Consumer<QueryResult> action);

    TSelf removeAfterQueryExecutedListener(Consumer<QueryResult> action);

    //TBD void AfterStreamExecuted(Action<BlittableJsonReaderObject> action);

    void invokeAfterQueryExecuted(QueryResult result);

    //TBD void InvokeAfterStreamExecuted(BlittableJsonReaderObject result);

    /**
     * Disables caching for query results.
     */
    TSelf noCaching();

    /**
     * Disables tracking for queried entities by Raven's Unit of Work.
     * Usage of this option will prevent holding query results in memory.
     */
    TSelf noTracking();

    //TBD TSelf showTimings();

    /**
     * Skips the specified count.
     */
    TSelf skip(int count);

    /**
     * Provide statistics about the query, such as total count of matching records
     */
    TSelf statistics(Reference<QueryStatistics> stats);

    /**
     * Takes the specified count.
     */
    TSelf take(int count);

    /**
     * Select the default operator to use for this query
     */
    TSelf usingDefaultOperator(QueryOperator queryOperator);

    /**
     * EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     */
    TSelf waitForNonStaleResults(Duration waitTimeout);

    /**
     * Create the index query object for this query
     */
    IndexQuery getIndexQuery();

    /**
     * Add a named parameter to the query
     */
    TSelf addParameter(String name, Object value);
}
