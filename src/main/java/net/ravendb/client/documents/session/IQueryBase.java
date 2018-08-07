package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.timings.QueryTimings;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.function.Consumer;

public interface IQueryBase<T, TSelf extends IQueryBase<T, TSelf>> {

    /**
     * Gets the document convention from the query session
     * @return document conventions
     */
    DocumentConventions getConventions();

    TSelf addBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    TSelf removeBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    TSelf addAfterQueryExecutedListener(Consumer<QueryResult> action);

    TSelf removeAfterQueryExecutedListener(Consumer<QueryResult> action);

    TSelf addAfterStreamExecutedListener(Consumer<ObjectNode> action);

    TSelf removeAfterStreamExecutedListener(Consumer<ObjectNode> action);

    void invokeAfterQueryExecuted(QueryResult result);

    void invokeAfterStreamExecuted(ObjectNode result);

    /**
     * Disables caching for query results.
     * @return Query instance
     */
    TSelf noCaching();

    /**
     * Disables tracking for queried entities by Raven's Unit of Work.
     * Usage of this option will prevent holding query results in memory.
     * @return Query instance
     */
    TSelf noTracking();

    /**
     *  Enables calculation of timings for various parts of a query (Lucene search, loading documents, transforming
     *  results). Default: false
     * @param timings Reference to output parameter
     * @return Query instance
     */
    TSelf timings(Reference<QueryTimings> timings);

    /**
     * Skips the specified count.
     * @param count Items to skip
     * @return Query instance
     */
    TSelf skip(int count);

    /**
     * Provide statistics about the query, such as total count of matching records
     * @param stats Output parameter for query stats
     * @return Query instance
     */
    TSelf statistics(Reference<QueryStatistics> stats);

    /**
     * Takes the specified count.
     * @param count Amount of items to take
     * @return Query instance
     */
    TSelf take(int count);

    /**
     * Select the default operator to use for this query
     * @param queryOperator Query operator to use
     * @return Query instance
     */
    TSelf usingDefaultOperator(QueryOperator queryOperator);

    /**
     * EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     * @return Query instance
     */
    TSelf waitForNonStaleResults();

    /**
     * EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     * @param waitTimeout Max wait timeout
     * @return Query instance
     */
    TSelf waitForNonStaleResults(Duration waitTimeout);

    /**
     * Create the index query object for this query
     * @return index query
     */
    IndexQuery getIndexQuery();

    /**
     * Add a named parameter to the query
     * @param name Parameter name
     * @param value Parameter value
     * @return Query instance
     */
    TSelf addParameter(String name, Object value);
}
