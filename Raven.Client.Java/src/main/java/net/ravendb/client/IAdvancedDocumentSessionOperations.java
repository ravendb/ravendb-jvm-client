package net.ravendb.client;

import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.commands.ICommandData;
import net.ravendb.abstractions.data.DocumentsChanges;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.exceptions.ConcurrencyException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.exceptions.NonAuthoritativeInformationException;


/**
 * Advanced session operations
 */
public interface IAdvancedDocumentSessionOperations {

  /**
   * The document store associated with this session
   */
  public IDocumentStore getDocumentStore();

  /**
   * Returns whatever a document with the specified id is loaded in the
   * current session
   * @param id
   */
  public boolean isLoaded(String id);

  /**
   * Gets a value indicating whether the session should use optimistic concurrency.
   * When set to true, a check is made so that a change made behind the session back would fail
   * and raise {@link ConcurrencyException}.
   */
  public String getStoreIdentifier();

  /**
   * Evicts the specified entity from the session.
   * Remove the entity from the delete queue and stops tracking changes for this entity.
   * @param entity Entity to evict.
   */
  public <T> void evict(T entity);


  /**
   * Removes the specify documentId from the list of known missing ids.
   * @param id documentId to unregister
     */
  public void unregisterMissing(String id);
  /**
   * Clears this instance.
   * Remove all entities from the delete queue and stops tracking changes for all entities.
   */
  public void clear();

  /**
   * Gets a value indicating whether the session should use optimistic concurrency.
   * When set to true, a check is made so that a change made behind the session back would fail
   * and raise {@link ConcurrencyException}.
   */
  public boolean isUseOptimisticConcurrency();

  /**
   * Sets a value indicating whether the session should use optimistic concurrency.
   * When set to true, a check is made so that a change made behind the session back would fail
   * and raise {@link ConcurrencyException}.
   */
  public void setUseOptimisticConcurrency(boolean value) ;

  /**
   * Allow extensions to provide additional state per session
   */
  public Map<String, Object> getExternalState();

  /**
   * Mark the entity as read only, change tracking won't apply
   * to such an entity. This can be done as an optimization step, so
   * we don't need to check the entity for changes.
   * @param entity
   */
  public void markReadOnly(Object entity);

  /**
   * Gets a value indicating whether non authoritative information is allowed.
   * Non authoritative information is document that has been modified by a transaction that hasn't been committed.
   * The server provides the latest committed version, but it is known that attempting to write to a non authoritative document
   * will fail, because it is already modified.
   * If set to false, the session will wait {@link #getNonAuthoritativeInformationTimeout()} for the transaction to commit to get an
   * authoritative information. If the wait is longer than {@link #getNonAuthoritativeInformationTimeout()}, {@link NonAuthoritativeInformationException} is thrown.
   * @return true if non authoritative information is allowed; otherwise, false.
   */
  public boolean isAllowNonAuthoritativeInformation();
  /**
   * Sets a value indicating whether non authoritative information is allowed.
   * Non authoritative information is document that has been modified by a transaction that hasn't been committed.
   * The server provides the latest committed version, but it is known that attempting to write to a non authoritative document
   * will fail, because it is already modified.
  *  If set to false, the session will wait {@link #getNonAuthoritativeInformationTimeout()} for the transaction to commit to get an
   * authoritative information. If the wait is longer than {@link #getNonAuthoritativeInformationTimeout()}, {@link NonAuthoritativeInformationException} is thrown.
   */
  public void setAllowNonAuthoritativeInformation(boolean value);

  /**
   * Gets the timeout to wait for authoritative information if encountered non authoritative document.
   */
  public Long getNonAuthoritativeInformationTimeout();

  /**
   * Sets the timeout to wait for authoritative information if encountered non authoritative document.
   * @param timeOutInMilis
   */
  public void setNonAuthoritativeInformationTimeout(Long timeOutInMilis);

  /**
   * Gets the max number of requests per session.
   * If the {@link #getNumberOfRequests()} rise above {@link #getMaxNumberOfRequestsPerSession()}, an exception will be thrown.
   */
  public int getMaxNumberOfRequestsPerSession();

  /**
   * Sets the max number of requests per session.
   * If the {@link #getNumberOfRequests()} rise above {@link #getMaxNumberOfRequestsPerSession()}, an exception will be thrown.
   * @param value
   */
  public void setMaxNumberOfRequestsPerSession(int value);

  /**
   * Gets the number of requests for this session
   * If the {@link NumberOfRequests} rise above {@link MaxNumberOfRequestsPerSession}, an exception will be thrown.
   */
  public int getNumberOfRequests();

  /**
   * Gets the metadata for the specified entity.
   * If the entity is transient, it will load the metadata from the store
   * and associate the current state of the entity with the metadata from the server.
   * @param instance The instance.
   */
  public <T> RavenJObject getMetadataFor(T instance);

  /**
   * Gets the ETag for the specified entity.
   * If the entity is transient, it will load the etag from the store
   * and associate the current state of the entity with the etag from the server.
   * @param instance The instance.
   */
  public <T> Etag getEtagFor(T instance);

  /**
   * Gets the document id for the specified entity.
   *
   * This function may return null if the entity isn't tracked by the session, or if the entity is
   * a new entity with a key that should be generated on the server.
   * @param entity The entity.
   */
  public String getDocumentId(Object entity);

  /**
   * Gets a value indicating whether any of the entities tracked by the session has changes.
   */
  public boolean hasChanges();

  /**
   * Determines whether the specified entity has changed.
   * @param entity
   * @return true if the specified entity has changed; otherwise, false.
   */
  public boolean hasChanged(Object entity);

  /**
   * Defer commands to be executed on saveChanges()
   * @param commands Array of commands to be executed.
   */
  public void defer(ICommandData... commands);

  /**
   * Version this entity when it is saved. Use when Versioning bundle configured to ExcludeUnlessExplicit.
   * @param entity Entity to version.
   */
  public void explicitlyVersion(Object entity);

  /**
   * Mark the entity as one that should be ignore for change tracking purposes,
   * it still takes part in the session, but is ignored for saveChanges.
   * @param entity
   */
  public void ignoreChangesFor(Object entity);

  /**
   * Returns all changes for each entity stored within session. Including name of the field/property that changed, its old and new value and change type.
   */
  public Map<String, List<DocumentsChanges>> whatChanged();

}
