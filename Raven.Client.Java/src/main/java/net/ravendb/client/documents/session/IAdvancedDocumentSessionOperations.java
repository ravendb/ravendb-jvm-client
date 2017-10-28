package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;

import java.util.List;
import java.util.Map;

/**
    Advanced session operations
 */
public interface IAdvancedDocumentSessionOperations {

    /**
     * The document store associated with this session
     */
    IDocumentStore getDocumentStore();

    /**
     * Allow extensions to provide additional state per session
     */
    Map<String, Object> getExternalState();

    ServerNode getCurrentSessionNode();

    RequestExecutor getRequestExecutor();

    /* TODO
     event EventHandler<BeforeStoreEventArgs> OnBeforeStore;
     event EventHandler<AfterStoreEventArgs> OnAfterStore;
     event EventHandler<BeforeDeleteEventArgs> OnBeforeDelete;
     event EventHandler<BeforeQueryExecutedEventArgs> OnBeforeQueryExecuted;
     */

    /**
     * Gets a value indicating whether any of the entities tracked by the session has changes.
     */
    boolean hasChanges();

    /**
     * Gets the max number of requests per session.
     */
    int getMaxNumberOfRequestsPerSession();

    /**
     * Sets the max number of requests per session.
     */
    void setMaxNumberOfRequestsPerSession(int maxRequests);

    /**
     * Gets the number of requests for this session
     */
    int getNumberOfRequests();
    /**
     * Gets the store identifier for this session.
     * The store identifier is the identifier for the particular RavenDB instance.
     */
    String storeIdentifier();

    /**
     * Gets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     */
    boolean isUseOptimisticConcurrency();

    /**
     * Sets value indicating whether the session should use optimistic concurrency.
     * When set to true, a check is made so that a change made behind the session back would fail
     * and raise ConcurrencyException
     */
    void setUseOptimisticConcurrency(boolean useOptimisticConcurrency);

    /**
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    void clear();

    /**
     * Defer commands to be executed on saveChanges()
     */
    void defer(ICommandData command, ICommandData... commands);

    /**
     * Defer commands to be executed on saveChanges()
     */
    void defer(ICommandData[] commands);

    /**
     * Evicts the specified entity from the session.
     * Remove the entity from the delete queue and stops tracking changes for this entity.
     */
    <T> void evict(T entity);

    /**
     * Gets the document id for the specified entity.
     *
     *  This function may return <c>null</c> if the entity isn't tracked by the session, or if the entity is
     *   a new entity with an ID that should be generated on the server.
     */
    String getDocumentId(Object entity);

    /* TODO:

     /// <summary>
     ///     Gets the metadata for the specified entity.
     ///     If the entity is transient, it will load the metadata from the store
     ///     and associate the current state of the entity with the metadata from the server.
     /// </summary>
     /// <param name="instance">The instance.</param>
     IMetadataDictionary GetMetadataFor<T>(T instance);
     */

    /**
     * Gets change vector for the specified entity.
     * If the entity is transient, it will load the metadata from the store
     * and associate the current state of the entity with the metadata from the server.
     */
    <T> String getChangeVectorFor(T instance);

    /**
     * Determines whether the specified entity has changed.
     */
    boolean hasChanged(Object entity);

    /**
     * Returns whether a document with the specified id is loaded in the
     * current session
     */
    boolean isLoaded(String id);

    /**
     * Mark the entity as one that should be ignore for change tracking purposes,
     * it still takes part in the session, but is ignored for SaveChanges.
     */
    void ignoreChangesFor(Object entity);

    /**
     * Returns all changes for each entity stored within session. Including name of the field/property that changed, its old and new value and change type.
     */
    Map<String, List<DocumentsChanges>> whatChanged();

    /* TODO - use options object ?

     /// <summary>
     /// SaveChanges will wait for the changes made to be replicates to `replicas` nodes
     /// </summary>
     void WaitForReplicationAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = true, int replicas = 1, bool majority = false);

     /// <summary>
     /// SaveChanges will wait for the indexes to catch up with the saved changes
     /// </summary>
     void WaitForIndexesAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = true, string[] indexes = null);

     */

    /**
     * Convert json to entity
     */
    Object convertToEntity(Class entityType, String id, ObjectNode documentFound);

    EntityToJson getEntityToJson();
}
