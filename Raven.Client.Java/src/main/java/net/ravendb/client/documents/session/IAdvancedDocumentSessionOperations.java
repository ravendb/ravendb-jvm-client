package net.ravendb.client.documents.session;

import net.ravendb.client.documents.IDocumentStore;

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

    /* TODO

     /// <summary>
     ///     Allow extensions to provide additional state per session
     /// </summary>
     IDictionary<string, object> ExternalState { get; }

     Task<ServerNode> GetCurrentSessionNode();

     RequestExecutor RequestExecutor { get; }
     JsonOperationContext Context { get; }

     event EventHandler<BeforeStoreEventArgs> OnBeforeStore;
     event EventHandler<AfterStoreEventArgs> OnAfterStore;
     event EventHandler<BeforeDeleteEventArgs> OnBeforeDelete;
     event EventHandler<BeforeQueryExecutedEventArgs> OnBeforeQueryExecuted;

     /// <summary>
     ///     Gets a value indicating whether any of the entities tracked by the session has changes.
     /// </summary>
     bool HasChanges { get; }

     /// <summary>
     ///     Gets or sets the max number of requests per session.
     ///     If the <see cref="NumberOfRequests" /> rise above <see cref="MaxNumberOfRequestsPerSession" />, an exception will
     ///     be thrown.
     /// </summary>
     /// <value>The max number of requests per session.</value>
     int MaxNumberOfRequestsPerSession { get; set; }

     */

    /**
     * Gets the number of requests for this session
     */
    int getNumberOfRequests();
    /**
     * Gets the store identifier for this session.
     * The store identifier is the identifier for the particular RavenDB instance.
     */
    String storeIdentifier();

    /* TODO:

     /// <summary>
     ///     Gets or sets a value indicating whether the session should use optimistic concurrency.
     ///     When set to <c>true</c>, a check is made so that a change made behind the session back would fail
     ///     and raise <see cref="ConcurrencyException" />.
     /// </summary>
     bool UseOptimisticConcurrency { get; set; }
*/

    /**
     * Clears this instance.
     * Remove all entities from the delete queue and stops tracking changes for all entities.
     */
    void clear();


    /* TODO

     /// <summary>
     ///     Defer commands to be executed on SaveChanges()
     /// </summary>
     /// <param name="command">Command to be executed</param>
     /// <param name="commands">Array of commands to be executed.</param>
     void Defer(ICommandData command, params ICommandData[] commands);

     /// <summary>
     ///     Defer commands to be executed on SaveChanges()
     /// </summary>
     /// <param name="commands">Array of commands to be executed.</param>
     void Defer(ICommandData[] commands);

     /// <summary>
     ///     Evicts the specified entity from the session.
     ///     Remove the entity from the delete queue and stops tracking changes for this entity.
     /// </summary>
     /// <param name="entity">Entity to evict.</param>
     void Evict<T>(T entity);

     */

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

    /* TODO

     /// <summary>
     /// SaveChanges will wait for the changes made to be replicates to `replicas` nodes
     /// </summary>
     void WaitForReplicationAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = true, int replicas = 1, bool majority = false);

     /// <summary>
     /// SaveChanges will wait for the indexes to catch up with the saved changes
     /// </summary>
     void WaitForIndexesAfterSaveChanges(TimeSpan? timeout = null, bool throwOnTimeout = true, string[] indexes = null);

     /// <summary>
     /// Convert blittable to entity
     /// </summary>
     /// <param name="entityType"></param>
     /// <param name="id"></param>
     /// <param name="documentFound"></param>
     /// <returns></returns>
     object ConvertToEntity(Type entityType, string id, BlittableJsonReaderObject documentFound);

     */

    EntityToJson getEntityToJson();
}
