package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.AdminOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.util.IDisposalNotification;

import java.util.List;

/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

    //TBD: X509Certificate2 Certificate { get; }

    void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);
    void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);

    void addAfterStoreListener(EventHandler<AfterStoreEventArgs> handler);
    void removeAfterStoreListener(EventHandler<AfterStoreEventArgs> handler);

    void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);
    void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);

    void addBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler);
    void removeBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler);

    //TBD: IDatabaseChanges Changes(string database = null);

    /* TODO:
        /// <summary>
        /// Setup the context for aggressive caching.
        /// </summary>
        /// <param name="cacheDuration">Specify the aggressive cache duration</param>
        /// <param name="database">The database to cache, if not specified, the default database will be used</param>
        /// <remarks>
        /// Aggressive caching means that we will not check the server to see whether the response
        /// we provide is current or not, but will serve the information directly from the local cache
        /// without touching the server.
        /// </remarks>
        IDisposable AggressivelyCacheFor(TimeSpan cacheDuration, string database = null);

        /// <summary>
        /// Setup the context for aggressive caching.
        /// </summary>
        /// <remarks>
        /// Aggressive caching means that we will not check the server to see whether the response
        /// we provide is current or not, but will serve the information directly from the local cache
        /// without touching the server.
        /// </remarks>
        IDisposable AggressivelyCache(string database = null);
        */

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    CleanCloseable disableAggressiveCaching();

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    CleanCloseable disableAggressiveCaching(String database);

    /**
     * @return Gets the identifier for this store.
     */
    String getIdentifier();

    /**
     * Sets the identifier for this store.
     */
    void setIdentifier(String identifier);

    /**
     * Initializes this instance.
     */
    IDocumentStore initialize();


    /**
     * Opens the session
     */
    IDocumentSession openSession();

    /**
     * Opens the session for a particular database
     */
    IDocumentSession openSession(String database);

    /**
     * Opens the session with the specified options.
     */
    IDocumentSession openSession(SessionOptions sessionOptions);

    /**
     * Executes the index creation
     */
    void executeIndex(AbstractIndexCreationTask task);

    void executeIndexes(List<AbstractIndexCreationTask> tasks);

    /**
     * Gets the conventions
     */
    DocumentConventions getConventions();

    /**
     * Gets the URL's
     */
    String[] getUrls();

    //TBD: BulkInsertOperation BulkInsert(string database = null);
    //TBD: IReliableSubscriptions Subscriptions { get; }

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    AdminOperationExecutor admin();

    OperationExecutor operations();

    //TODO:IDisposable SetRequestsTimeout(TimeSpan timeout, string database = null);
}
