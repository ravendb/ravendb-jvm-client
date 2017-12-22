package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.util.IDisposalNotification;

import java.security.KeyStore;
import java.util.List;

/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

    KeyStore getCertificate();

    void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);
    void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);

    void addAfterStoreListener(EventHandler<AfterStoreEventArgs> handler);
    void removeAfterStoreListener(EventHandler<AfterStoreEventArgs> handler);

    void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);
    void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);

    void addBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler);
    void removeBeforeQueryExecutedListener(EventHandler<BeforeQueryExecutedEventArgs> handler);

    //TBD: IDatabaseChanges Changes(string database = null);
    //TBD: IDisposable AggressivelyCacheFor(TimeSpan cacheDuration, string database = null);
    //TBD IDisposable AggressivelyCache(string database = null);

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     * @return Self closing context
     */
    CleanCloseable disableAggressiveCaching();

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     * @param database Database name
     * @return Self closing context
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
    void executeIndex(AbstractIndexCreationTask task);

    /**
     * Executes the index creation
     * @param task Index Creation task to use
     * @param database Target database
     */
    void executeIndex(AbstractIndexCreationTask task, String database);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     */
    void executeIndexes(List<AbstractIndexCreationTask> tasks);

    /**
     * Executes the index creation
     * @param tasks Index Creation tasks to use
     * @param database Target database
     */
    void executeIndexes(List<AbstractIndexCreationTask> tasks, String database);

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

    //TBD: BulkInsertOperation BulkInsert(string database = null);
    //TBD: IReliableSubscriptions Subscriptions { get; }

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    MaintenanceOperationExecutor maintenance();

    OperationExecutor operations();

}
