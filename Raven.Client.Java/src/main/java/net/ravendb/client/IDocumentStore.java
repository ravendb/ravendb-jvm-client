package net.ravendb.client;

import java.util.Date;
import java.util.Map;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.BulkInsertOperation;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.DocumentSessionListeners;
import net.ravendb.client.document.IReliableSubscriptions;
import net.ravendb.client.document.OpenSessionOptions;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;


/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

  /**
   * Subscribe to change notifications from the server
   */
  public IDatabaseChanges changes();

  /**
   * Subscribe to change notifications from the server
   * @param database
   */
  public IDatabaseChanges changes(String database);

  /**
   * Setup the context for aggressive caching.
   *
   * Aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   * @param cacheDurationInMilis
   */
  public CleanCloseable aggressivelyCacheFor(long cacheDurationInMilis);

  /**
   * Setup the context for aggressive caching.
   *
   * Aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   */
  public CleanCloseable aggressivelyCache();

  /**
   * Setup the context for no aggressive caching
   *
   * This is mainly useful for internal use inside RavenDB, when we are executing
   * queries that has been marked with WaitForNonStaleResults, we temporarily disable
   * aggressive caching.
   */
  public CleanCloseable disableAggressiveCaching();

  /**
   * Setup the WebRequest timeout for the session
   * @param timeout Specify the timeout duration
   * @return Sets the timeout for the JsonRequest.  Scoped to the Current Thread.
   */
  public CleanCloseable setRequestsTimeoutFor(long timeout);

  /**
   * Gets the shared operations headers.
   */
  public Map<String, String> getSharedOperationsHeaders();


  /**
   * Get the {@link HttpJsonRequestFactory} for this store
   */
  public HttpJsonRequestFactory getJsonRequestFactory();

  /**
   * Whatever this instance has json request factory available
   */
  public boolean hasJsonRequestFactory();

  /**
   * Sets the identifier
   * @param identifier
   */
  public void setIdentifier(String identifier);

  /**
   * Gets the identifier
   */
  public String getIdentifier();

  /**
   * Initializes this instance.
   */
  public IDocumentStore initialize();

  /**
   * Opens the session.
   */
  public IDocumentSession openSession();

  /**
   * Opens the session for a particular database
   * @param database
   */
  public IDocumentSession openSession(String database);

  /**
   * Opens the session with the specified options.
   * @param sessionOptions
   */
  public IDocumentSession openSession(OpenSessionOptions sessionOptions);

  /**
   * Gets the database commands.
   */
  public IDatabaseCommands getDatabaseCommands();

  /**
   * Executes the index creation in side-by-side mode.
   * @param indexCreationTask
   * @param minimumEtagBeforeReplace
   * @param replaceTimeUtc
   */
  public void sideBySideExecuteIndex(AbstractIndexCreationTask indexCreationTask);

  /**
   * Executes the index creation in side-by-side mode.
   * @param indexCreationTask
   * @param minimumEtagBeforeReplace
   * @param replaceTimeUtc
   */
  public void sideBySideExecuteIndex(AbstractIndexCreationTask indexCreationTask, Etag minimumEtagBeforeReplace, Date replaceTimeUtc);

  /**
   * Executes the index creation.
   * @param indexCreationTask
   */
  public void executeIndex(AbstractIndexCreationTask indexCreationTask);

  /**
   * executes the transformer creation
   * @param transformerCreationTask
   */
  public void executeTransformer(AbstractTransformerCreationTask transformerCreationTask);

  /**
   * Gets the conventions.
   */
  public DocumentConvention getConventions();

  /**
   * Gets the URL.
   */
  public String getUrl();

  /**
   * Gets the etag of the last document written by any session belonging to this
   * document store
   */
  public Etag getLastWrittenEtag();

  /**
   * Performs bulk insert
   */
  public BulkInsertOperation bulkInsert();

  /**
   * Performs bulk insert
   * @param database
   */
  public BulkInsertOperation bulkInsert(String database);

  /**
   * Performs bulk insert
   * @param database
   * @param options
   */
  public BulkInsertOperation bulkInsert(String database, BulkInsertOptions options);

  public DocumentSessionListeners getListeners();

  public IReliableSubscriptions subscriptions();

  public void setListeners(DocumentSessionListeners listeners);

}
