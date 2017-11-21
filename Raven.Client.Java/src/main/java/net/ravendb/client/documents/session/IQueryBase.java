package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperator;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.primitives.Reference;

import java.util.function.Consumer;

public interface IQueryBase<T, TSelf extends IQueryBase<T, TSelf>> {

    /**
     * Gets the document convention from the query session
     */
    DocumentConventions getConventions();

    void addBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    void removeBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    void addAfterQueryExecutedListener(Consumer<QueryResult> action);

    void removeAfterQueryExecutedListener(Consumer<QueryResult> action);

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

    /**
     * Enables calculation of timings for various parts of a query (Lucene search, loading documents, transforming
     * results). Default: false
     */
    TSelf showTimings();

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
     * EXPERT ONLY: Instructs the query to wait for non stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     */
    TSelf waitForNonStaleResults();

    /* TODO
        /// <summary>
        ///     EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
        ///     This shouldn't be used outside of unit tests unless you are well aware of the implications
        /// </summary>
        /// <param name="waitTimeout">Maximum time to wait for index query results to become non-stale before exception is thrown.</param>
        TSelf WaitForNonStaleResults(TimeSpan waitTimeout);

        /// <summary>
        ///     Instructs the query to wait for non stale results as of the cutoff etag.
        /// </summary>
        /// <param name="cutOffEtag">
        ///     <para>Cutoff etag is used to check if the index has already process a document with the given</para>
        ///     <para>etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between</para>
        ///     <para>machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and </para>
        ///     <para>can work without it.</para>
        ///     <para>However, when used to query map/reduce indexes, it does NOT guarantee that the document that this</para>
        ///     <para>etag belong to is actually considered for the results. </para>
        ///     <para>What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced. </para>
        ///     <para>Since map/reduce queries, by their nature, tend to be far less susceptible to issues with staleness, this is </para>
        ///     <para>considered to be an acceptable trade-off.</para>
        ///     <para>If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and </para>
        ///     <para>use the Cutoff date option, instead.</para>
        /// </param>
        TSelf WaitForNonStaleResultsAsOf(long cutOffEtag);

        /// <summary>
        ///     Instructs the query to wait for non stale results as of the cutoff etag for the specified timeout.
        /// </summary>
        /// <param name="cutOffEtag">
        ///     <para>Cutoff etag is used to check if the index has already process a document with the given</para>
        ///     <para>etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between</para>
        ///     <para>machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and </para>
        ///     <para>can work without it.</para>
        ///     <para>However, when used to query map/reduce indexes, it does NOT guarantee that the document that this</para>
        ///     <para>etag belong to is actually considered for the results. </para>
        ///     <para>What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced. </para>
        ///     <para>Since map/reduce queries, by their nature, tend to be far less susceptible to issues with staleness, this is </para>
        ///     <para>considered to be an acceptable trade-off.</para>
        ///     <para>If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and </para>
        ///     <para>use the Cutoff date option, instead.</para>
        /// </param>
        /// <param name="waitTimeout">Maximum time to wait for index query results to become non-stale before exception is thrown.</param>
        TSelf WaitForNonStaleResultsAsOf(long cutOffEtag, TimeSpan waitTimeout);

     */

    /**
     * Create the index query object for this query
     */
    IndexQuery getIndexQuery();
}
