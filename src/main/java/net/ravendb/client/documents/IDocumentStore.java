package net.ravendb.client.documents;

import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTaskBase;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.smuggler.DatabaseSmuggler;
import net.ravendb.client.documents.subscriptions.DocumentSubscriptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.util.IDisposalNotification;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

    KeyStore getCertificate();

    void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);
    void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);

    void addAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);
    void removeAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);

    void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);
    void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);

    void addBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);
    void removeBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);

    /**
     * Subscribe to change notifications from the server
     * @return Database changes object
     */
    IDatabaseChanges changes();

    /**
     * Subscribe to change notifications from the server
     * @param database Database to use
     * @return Database changes object
     */
    IDatabaseChanges changes(String database);

    /**
     * Setup the context for aggressive caching.
     *
     * Aggressive caching means that we will not check the server to see whether the response
     * we provide is current or not, but will serve the information directly from the local cache
     * without touching the server.
     *
     * @param cacheDuration Specify the aggressive cache duration
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCacheFor(Duration cacheDuration);

    /**
     * Setup the context for aggressive caching.
     *
     * Aggressive caching means that we will not check the server to see whether the response
     * we provide is current or not, but will serve the information directly from the local cache
     * without touching the server.
     *
     * @param cacheDuration Specify the aggressive cache duration
     * @param database The database to cache, if not specified, the default database will be used
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCacheFor(Duration cacheDuration, String database);


    /**
     * Setup the context for aggressive caching.
     *
     * Aggressive caching means that we will not check the server to see whether the response
     * we provide is current or not, but will serve the information directly from the local cache
     * without touching the server.
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCache();

    /**
     * Setup the context for aggressive caching.
     *
     * Aggressive caching means that we will not check the server to see whether the response
     * we provide is current or not, but will serve the information directly from the local cache
     * without touching the server.
     *
     * @param database The database to cache, if not specified, the default database will be used
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCache(String database);

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     * @return Context for aggressive caching
     */
    CleanCloseable disableAggressiveCaching();

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     * @param database Database name
     * @return Context for aggressive caching
     */
    CleanCloseable disableAggressiveCaching(String database);

    /**
     * @return Gets the identifier for this store.
     */
    String getIdentifier();

    /**
     * Sets the identifier for this store.
     * @param identifier Identifier to set
     */
    void setIdentifier(String identifier);

    /**
     * Initializes this instance.
     * @return initialized store
     */
    IDocumentStore initialize();


    /**
     * Opens the session
     * @return Document session
     */
    IDocumentSession openSession();

    /**
     * Opens the session for a particular database
     * @param database Database to use
     * @return Document session
     */
    IDocumentSession openSession(String database);

    /**
     * Opens the session with the specified options.
     * @param sessionOptions Session options to use
     * @return Document session
     */
    IDocumentSession openSession(SessionOptions sessionOptions);

    /**
     * Executes the index creation
     * @param task Index Creation task to use
     */
    void executeIndex(AbstractIndexCreationTaskBase task);

    /**
     * Executes the index creation
     * @param task Index Creation task to use
     * @param database Target database
     */
    void executeIndex(AbstractIndexCreationTaskBase task, String database);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     */
    void executeIndexes(List<AbstractIndexCreationTaskBase> tasks);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     * @param database Target database
     */
    void executeIndexes(List<AbstractIndexCreationTaskBase> tasks, String database);

    /**
     * Gets the conventions
     * @return Document conventions
     */
    DocumentConventions getConventions();

    /**
     * Gets the URL's
     * @return Store urls
     */
    String[] getUrls();

    BulkInsertOperation bulkInsert();

    BulkInsertOperation bulkInsert(String database);

    DocumentSubscriptions subscriptions();

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    MaintenanceOperationExecutor maintenance();

    OperationExecutor operations();

    DatabaseSmuggler smuggler();
}
