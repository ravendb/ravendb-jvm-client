package net.ravendb.client.documents;

import net.ravendb.client.documents.bulkInsert.BulkInsertOptions;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.identity.HiLoIdGenerator;
import net.ravendb.client.documents.identity.IHiLoIdGenerator;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTaskBase;
import net.ravendb.client.documents.indexes.IAbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.smuggler.DatabaseSmuggler;
import net.ravendb.client.documents.subscriptions.DocumentSubscriptions;
import net.ravendb.client.documents.timeSeries.TimeSeriesOperations;
import net.ravendb.client.http.AggressiveCacheMode;
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

    IHiLoIdGenerator getHiLoIdGenerator();

    void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);
    void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);

    void addAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);
    void removeAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);

    void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);
    void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);

    void addBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);
    void removeBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);

    void addBeforeConversionToDocumentListener(EventHandler<BeforeConversionToDocumentEventArgs> handler);
    void removeBeforeConversionToDocumentListener(EventHandler<BeforeConversionToDocumentEventArgs> handler);

    void addAfterConversionToDocumentListener(EventHandler<AfterConversionToDocumentEventArgs> handler);
    void removeAfterConversionToDocumentListener(EventHandler<AfterConversionToDocumentEventArgs> handler);

    void addBeforeConversionToEntityListener(EventHandler<BeforeConversionToEntityEventArgs> handler);
    void removeBeforeConversionToEntityListener(EventHandler<BeforeConversionToEntityEventArgs> handler);

    void addAfterConversionToEntityListener(EventHandler<AfterConversionToEntityEventArgs> handler);
    void removeAfterConversionToEntityListener(EventHandler<AfterConversionToEntityEventArgs> handler);

    void addOnBeforeRequestListener(EventHandler<BeforeRequestEventArgs> handler);
    void removeOnBeforeRequestListener(EventHandler<BeforeRequestEventArgs> handler);

    void addOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler);
    void removeOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler);

    void addOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler);
    void removeOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler);

    void addOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler);
    void removeOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler);

    void addOnSessionClosingListener(EventHandler<SessionClosingEventArgs> handler);
    void removeOnSessionClosingListener(EventHandler<SessionClosingEventArgs> handler);

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
     * Subscribe to change notifications from the server
     * @param database Database to use
     * @param nodeTag The node tag of selected server
     * @return Database changes object
     */
    IDatabaseChanges changes(String database, String nodeTag);

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
     *
     * @param cacheDuration Specify the aggressive cache duration
     * @param mode Aggressive caching mode, if not specified, TrackChanges mode will be used
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCacheFor(Duration cacheDuration, AggressiveCacheMode mode);

    /**
     * Setup the context for aggressive caching.
     *
     * Aggressive caching means that we will not check the server to see whether the response
     * we provide is current or not, but will serve the information directly from the local cache
     * without touching the server.
     *
     * @param cacheDuration Specify the aggressive cache duration
     * @param mode Aggressive caching mode, if not specified, TrackChanges mode will be used
     * @param database The database to cache, if not specified, the default database will be used
     * @return Context for aggressive caching
     */
    CleanCloseable aggressivelyCacheFor(Duration cacheDuration, AggressiveCacheMode mode, String database);

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
    void executeIndex(IAbstractIndexCreationTask task);

    /**
     * Executes the index creation
     * @param task Index Creation task to use
     * @param database Target database
     */
    void executeIndex(IAbstractIndexCreationTask task, String database);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     */
    void executeIndexes(List<IAbstractIndexCreationTask> tasks);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     * @param database Target database
     */
    void executeIndexes(List<IAbstractIndexCreationTask> tasks, String database);

    TimeSeriesOperations timeSeries();

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

    BulkInsertOperation bulkInsert(String database, BulkInsertOptions options);

    BulkInsertOperation bulkInsert(BulkInsertOptions options);

    DocumentSubscriptions subscriptions();

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    MaintenanceOperationExecutor maintenance();

    OperationExecutor operations();

    DatabaseSmuggler smuggler();

    CleanCloseable setRequestTimeout(Duration timeout);

    CleanCloseable setRequestTimeout(Duration timeout, String database);
}
