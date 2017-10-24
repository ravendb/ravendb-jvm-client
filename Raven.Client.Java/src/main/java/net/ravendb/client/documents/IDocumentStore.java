package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AdminOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.util.IDisposalNotification;

/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

    //TODO: X509Certificate2 Certificate { get; }
    //TODO: event EventHandler<BeforeStoreEventArgs> OnBeforeStore;
    //TODO: event EventHandler<AfterStoreEventArgs> OnAfterStore;
    //TODO: event EventHandler<BeforeDeleteEventArgs> OnBeforeDelete;
    //TODO: event EventHandler<BeforeQueryExecutedEventArgs> OnBeforeQueryExecuted;

    //TODO: IDatabaseChanges Changes(string database = null);

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

        /// <summary>
        /// Setup the context for no aggressive caching
        /// </summary>
        /// <remarks>
        /// This is mainly useful for internal use inside RavenDB, when we are executing
        /// queries that has been marked with WaitForNonStaleResults, we temporarily disable
        /// aggressive caching.
        /// </remarks>
        IDisposable DisableAggressiveCaching(string database = null);
*/

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



    /* TODO
        /// <summary>
        /// Opens the session.
        /// </summary>
        /// <returns></returns>
        IDocumentSession OpenSession();

        /// <summary>
        /// Opens the session for a particular database
        /// </summary>
        IDocumentSession OpenSession(string database);

        /// <summary>
        /// Opens the session with the specified options.
        /// </summary>
        IDocumentSession OpenSession(SessionOptions sessionOptions);

        /// <summary>
        /// Executes the index creation.
        /// </summary>
        void ExecuteIndex(AbstractIndexCreationTask task);

        void ExecuteIndexes(IEnumerable<AbstractIndexCreationTask> tasks);

*/

    /**
     * Gets the conventions
     */
    DocumentConventions getConventions();

    /**
     * Gets the URL's
     */
    String[] getUrls();

    //TODO: BulkInsertOperation BulkInsert(string database = null);
    //TODO: IReliableSubscriptions Subscriptions { get; }

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    AdminOperationExecutor admin();

    OperationExecutor operations();

    //TODO:IDisposable SetRequestsTimeout(TimeSpan timeout, string database = null);
}
