package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.EventHandler;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
    Advanced session operations
 */
public interface IAdvancedDocumentSessionOperations {

    /**
     * The document store associated with this session
     * @return Document store
     */
    IDocumentStore getDocumentStore();

    /**
     * Allow extensions to provide additional state per session
     * @return External state
     */
    Map<String, Object> getExternalState();

    ServerNode getCurrentSessionNode();

    RequestExecutor getRequestExecutor();

    void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);
    void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler);

    void addAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);
    void removeAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler);

    void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);
    void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler);

    void addBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);
    void removeBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler);

    /**
     * Gets a value indicating whether any of the entities tracked by the session has changes.
     * @return true if any entity associated with session has changes
     */
    boolean hasChanges();

    /**
     * Gets the max number of requests per session.
     * @return maximum number of requests per session
     */
    int getMaxNumberOfRequestsPerSession();

    /**
     * Sets the max number of requests per session.
     * @param maxRequests Sets the maximum requests
     */
    void setMaxNumberOfRequestsPerSession(int maxRequests);

    /**
     * Gets the number of requests for this session
     * @return Number of requests issued on this session
     */
    int getNumberOfRequests();
    /**
     * Gets the store identifier for this session.
     * The store identifier is the identifier for the particular RavenDB instance.
     * @return Store identifier
     */
    String storeIdentifier();

    /**
     * Gets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     * @return true if optimistic concurrency should be used
     */
    boolean isUseOptimisticConcurrency();

    /**
     * Sets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     * @param useOptimisticConcurrency Sets the optimistic concurrency
     */
    void setUseOptimisticConcurrency(boolean useOptimisticConcurrency);

    /**
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    void clear();

    /**
     * Defer commands to be executed on saveChanges()
     * @param command command
     * @param commands more commands to defer
     */
    void defer(ICommandData command, ICommandData... commands);

    /**
     * Defer commands to be executed on saveChanges()
     * @param commands Commands to defer
     */
    void defer(ICommandData[] commands);

    /**
     * Evicts the specified entity from the session.
     * Remove the entity from the delete queue and stops tracking changes for this entity.
     * @param <T> entity class
     * @param entity Entity to evict
     */
    <T> void evict(T entity);

    /**
     * Gets the document id for the specified entity.
     *
     *  This function may return null if the entity isn't tracked by the session, or if the entity is
     *   a new entity with an ID that should be generated on the server.
     * @param entity Entity to get id from
     * @return document id
     */
    String getDocumentId(Object entity);

    /**
     * Gets the metadata for the specified entity.
     * If the entity is transient, it will load the metadata from the store
     * and associate the current state of the entity with the metadata from the server.
     * @param <T> class of instance
     * @param instance instance to get metadata from
     * @return Entity metadata
     */
    <T> IMetadataDictionary getMetadataFor(T instance);

    /**
     * Gets change vector for the specified entity.
     * If the entity is transient, it will load the metadata from the store
     * and associate the current state of the entity with the metadata from the server.
     * @param <T> Class of instance
     * @param instance Instance to get metadata from
     * @return Change vector
     */
    <T> String getChangeVectorFor(T instance);

    /**
     * Gets all the counter names for the specified entity.
     * @param instance The instance
     * @param <T> Class of instance
     * @return List of counter names
     */
    <T> List<String> getCountersFor(T instance);

    /**
     * Gets last modified date for the specified entity.
     * If the entity is transient, it will load the metadata from the store
     * and associate the current state of the entity with the metadata from the server.
     * @param instance Instance to get last modified date from
     * @param <T> Class of instance
     * @return Last modified date
     */
    <T> Date getLastModifiedFor(T instance);

    /**
     * Determines whether the specified entity has changed.
     * @param entity Entity to check
     * @return true if entity has changed
     */
    boolean hasChanged(Object entity);

    /**
     * Returns whether a document with the specified id is loaded in the
     * current session
     * @param id Id of document
     * @return true is entity is loaded in session
     */
    boolean isLoaded(String id);

    /**
     * Mark the entity as one that should be ignore for change tracking purposes,
     * it still takes part in the session, but is ignored for SaveChanges.
     * @param entity Entity for which changed should be ignored
     */
    void ignoreChangesFor(Object entity);

    /**
     * Returns all changes for each entity stored within session. Including name of the field/property that changed, its old and new value and change type.
     * @return Document changes
     */
    Map<String, List<DocumentsChanges>> whatChanged();

    /**
     * SaveChanges will wait for the changes made to be replicates to `replicas` nodes
     */
    void waitForReplicationAfterSaveChanges();

    /**
     * SaveChanges will wait for the changes made to be replicates to `replicas` nodes
     * @param options Configuration options
     */
    void waitForReplicationAfterSaveChanges(Consumer<InMemoryDocumentSessionOperations.ReplicationWaitOptsBuilder> options);

    /**
     * SaveChanges will wait for the indexes to catch up with the saved changes
     */
    void waitForIndexesAfterSaveChanges();

    /**
     * SaveChanges will wait for the indexes to catch up with the saved changes
     * @param options Configuration options
     */
    void waitForIndexesAfterSaveChanges(Consumer<InMemoryDocumentSessionOperations.IndexesWaitOptsBuilder> options);

    /**
     * Overwrite the existing transaction mode for the current session.
     * @param mode Transaction mode
     */
    void setTransactionMode(TransactionMode mode);

    EntityToJson getEntityToJson();
}
