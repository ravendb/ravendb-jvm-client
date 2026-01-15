package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.ProjectionBehavior;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.timings.QueryTimings;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.documents.session.querying.sharding.IQueryShardedContextBuilder;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.DocumentationUrls;
import java.time.Duration;
import java.util.function.Consumer;

public interface IDocumentQueryCustomization {

    /**
     * Get the raw query operation that will be sent to the server.
     * @return Query operation
     */
    QueryOperation getQueryOperation();

    /**
     * Get current Query
     * @return Query
     */
    AbstractDocumentQuery<?, ?> getQuery();

    /**
     * Allows to modify the index query before it is executed.
     * @param action Action with index query parameter. Defines the method that will be executed before query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#BeforeQueryExecuted
     */
    IDocumentQueryCustomization addBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    /**
     * Allows to modify the index query before it is executed.
     * @param action Action with index query parameter. Defines the method that will be executed before query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#BeforeQueryExecuted
     */
    IDocumentQueryCustomization removeBeforeQueryExecutedListener(Consumer<IndexQuery> action);

    /**
     * Allows to access raw query result after the execution.
     * @param action Action with query result parameter. Defines the method that will be executed after query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#AfterQueryExecuted
     */
    IDocumentQueryCustomization addAfterQueryExecutedListener(Consumer<QueryResult> action);

    /**
     * Allows to access raw query result after the execution.
     * @param action Action with query result parameter. Defines the method that will be executed after query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#AfterQueryExecuted
     */
    IDocumentQueryCustomization removeAfterQueryExecutedListener(Consumer<QueryResult> action);


    /**
     * Allows to access raw streaming query result after the query execution.
     * @param action Action with stream result parameter. Defines the method that will be executed after streaming query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#AfterStreamExecuted
     */
    IDocumentQueryCustomization addAfterStreamExecutedCallback(Consumer<ObjectNode> action);

    /**
     * Allows to access raw streaming query result after the query execution.
     * @param action Action with stream result parameter. Defines the method that will be executed after streaming query execution.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#AfterStreamExecuted
     */
    IDocumentQueryCustomization removeAfterStreamExecutedCallback(Consumer<ObjectNode> action);

    /**
     * Disables caching of query results.
     * Forces RavenDB to always fetch query results from the server.
     * By default query results are cached.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#NoCaching
     */
    IDocumentQueryCustomization noCaching();

    /**
     * Disables tracking of query results.
     * Any changes made to them will be ignored by RavenDB.
     * Usage of this option will prevent holding query results in memory.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.HowToCustomizeQuery#NoTracking
     */
    IDocumentQueryCustomization noTracking();

    /**
     * Orders the query results randomly.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.SortQueryResults#OrderByRandom
     */
    IDocumentQueryCustomization randomOrdering();

    /**
     * Orders the query results randomly using the specified seed.
     * Allows to repeat random query results.
     * @param seed Seed to be used for pseudorandom number generator.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying.SortQueryResults#OrderByRandom
     */
    IDocumentQueryCustomization randomOrdering(String seed);

    //TBD 4.1 IDocumentQueryCustomization CustomSortUsing(string typeName);
    //TBD 4.1 IDocumentQueryCustomization CustomSortUsing(string typeName, bool descending);
    /**
     * {@inheritDoc}
     * @see IQueryBase#timings(Reference)
     */
    IDocumentQueryCustomization timings(Reference<QueryTimings> timings);

    /**
     * Instruct the query to wait for non-stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications.
     * @return customization object
     */
    IDocumentQueryCustomization waitForNonStaleResults();

    /**
     * Instruct the query to wait for non-stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications.
     * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown. Default: 15 seconds.
     * @return customization object
     */
    IDocumentQueryCustomization waitForNonStaleResults(Duration waitTimeout);
    /**
     * {@inheritDoc}
     * @see IRawDocumentQuery#projection
     */
    IDocumentQueryCustomization projection(ProjectionBehavior projectionBehavior);

    /**
     * Allows to execute query only on relevant shards.
     * @param builder Action with shard context parameter. Defines on which shards the query will be executed.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Sharding#QueryingASelectedShard
     */
    IDocumentQueryCustomization shardContext(Consumer<IQueryShardedContextBuilder> builder);
}
