package net.ravendb.client;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.*;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;

import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {

  /**
   * Subscribe to change notifications from the server
   */
  IDatabaseChanges changes();

  /**
   * Subscribe to change notifications from the server
   * @param database
   */
  IDatabaseChanges changes(String database);

  /**
   * Setup the context for aggressive caching.
   *
   * Aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   * @param cacheDurationInMilis
   */
  CleanCloseable aggressivelyCacheFor(long cacheDurationInMilis);

  /**
   * Setup the context for aggressive caching.
   *
   * Aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   */
  CleanCloseable aggressivelyCache();

  /**
   * Setup the context for no aggressive caching
   *
   * This is mainly useful for internal use inside RavenDB, when we are executing
   * queries that has been marked with WaitForNonStaleResults, we temporarily disable
   * aggressive caching.
   */
  CleanCloseable disableAggressiveCaching();

  /**
   * Setup the WebRequest timeout for the session
   * @param timeout Specify the timeout duration
   * @return Sets the timeout for the JsonRequest.  Scoped to the Current Thread.
   */
  CleanCloseable setRequestsTimeoutFor(long timeout);

  /**
   * Gets the shared operations headers.
   */
  Map<String, String> getSharedOperationsHeaders();


  /**
   * Get the {@link HttpJsonRequestFactory} for this store
   */
  HttpJsonRequestFactory getJsonRequestFactory();

  /**
   * Whatever this instance has json request factory available
   */
  boolean hasJsonRequestFactory();

  /**
   * Sets the identifier
   * @param identifier
   */
  void setIdentifier(String identifier);

  /**
   * Gets the identifier
   */
  String getIdentifier();

  /**
   * Initializes this instance.
   */
  IDocumentStore initialize();

  /**
   * Opens the session.
   */
  IDocumentSession openSession();

  /**
   * Opens the session for a particular database
   * @param database
   */
  IDocumentSession openSession(String database);

  /**
   * Opens the session with the specified options.
   * @param sessionOptions
   */
  IDocumentSession openSession(OpenSessionOptions sessionOptions);

  /**
   * Gets the database commands.
   */
  IDatabaseCommands getDatabaseCommands();

  /**
   * Executes the index creation in side-by-side mode.
   * @param indexCreationTask
   */
  void sideBySideExecuteIndex(AbstractIndexCreationTask indexCreationTask);

  /**
   * Executes the index creation in side-by-side mode.
   * @param indexCreationTask
   * @param minimumEtagBeforeReplace
   * @param replaceTimeUtc
   */
  void sideBySideExecuteIndex(AbstractIndexCreationTask indexCreationTask, Etag minimumEtagBeforeReplace, Date replaceTimeUtc);

  void sideBySideExecuteIndexes(List<AbstractIndexCreationTask> indexCreationTasks);

  void sideBySideExecuteIndexes(List<AbstractIndexCreationTask> indexCreationTasks, Etag minimumEtagBeforeReplace, Date replaceTimeUtc);

  /**
   * Executes the index creation.
   * @param indexCreationTask
   */
  void executeIndex(AbstractIndexCreationTask indexCreationTask);


  void executeIndexes(List<AbstractIndexCreationTask> indexCreationTasks);

  /**
   * executes the transformer creation
   * @param transformerCreationTask
   */
  void executeTransformer(AbstractTransformerCreationTask transformerCreationTask);

  /**
   * Gets the conventions.
   */
  DocumentConvention getConventions();

  /**
   * Gets the URL.
   */
  String getUrl();

  /**
   * Gets the etag of the last document written by any session belonging to this
   * document store
   */
  Etag getLastWrittenEtag();

  /**
   * Performs bulk insert
   */
  BulkInsertOperation bulkInsert();

  /**
   * Performs bulk insert
   * @param database
   */
  BulkInsertOperation bulkInsert(String database);

  /**
   * Performs bulk insert
   * @param database
   * @param options
   */
  BulkInsertOperation bulkInsert(String database, BulkInsertOptions options);

  DocumentSessionListeners getListeners();

  IReliableSubscriptions subscriptions();

  void initializeProfiling();

  void setListeners(DocumentSessionListeners listeners);

}
