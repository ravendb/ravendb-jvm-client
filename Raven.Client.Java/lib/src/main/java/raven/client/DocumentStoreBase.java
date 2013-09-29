package raven.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import raven.abstractions.basic.EventHelper;
import raven.abstractions.closure.Action1;
import raven.abstractions.data.Etag;
import raven.abstractions.data.FailoverServers;
import raven.client.connection.profiling.ProfilingContext;
import raven.client.connection.profiling.ProfilingInformation;
import raven.client.document.DocumentConvention;
import raven.client.document.DocumentSessionListeners;
import raven.client.document.DocumentStore;
import raven.client.document.InMemoryDocumentSessionOperations;
import raven.client.document.dtc.ITransactionRecoveryStorage;
import raven.client.document.dtc.VolatileOnlyTransactionRecoveryStorage;
import raven.client.indexes.AbstractIndexCreationTask;
import raven.client.indexes.AbstractTransformerCreationTask;
import raven.client.listeners.IDocumentConflictListener;
import raven.client.listeners.IDocumentConversionListener;
import raven.client.listeners.IDocumentDeleteListener;
import raven.client.listeners.IDocumentQueryListener;
import raven.client.listeners.IDocumentStoreListener;
import raven.client.listeners.IExtendedDocumentConversionListener;
import raven.client.util.GlobalLastEtagHolder;
import raven.client.util.ILastEtagHolder;

/**
 * Contains implementation of some IDocumentStore operations shared by DocumentStore implementations
 */
public abstract class DocumentStoreBase implements IDocumentStore {
  protected DocumentStoreBase() {
    lastEtagHolder = new GlobalLastEtagHolder();
    transactionRecoveryStorage = new VolatileOnlyTransactionRecoveryStorage();
  }

  private boolean wasDisposed;
  private Map<String, String> sharedOperationsHeaders;
  protected DocumentConvention conventions;
  protected String url;
  private UUID resourceManagerId;
  protected boolean initialized;
  protected final DocumentSessionListeners listeners = new DocumentSessionListeners();
  protected ProfilingContext profilingContext = new ProfilingContext();
  private ILastEtagHolder lastEtagHolder;
  private ITransactionRecoveryStorage transactionRecoveryStorage;
  private List<Action1<InMemoryDocumentSessionOperations>> sessionCreatedInternal = new ArrayList<>();
  protected FailoverServers failoverServers;

  public FailoverServers getFailoverServers() {
    return failoverServers;
  }

  public void setFailoverServers(FailoverServers failoverServers) {
    this.failoverServers = failoverServers;
  }

  public void addSessionCreatedInternal(Action1<InMemoryDocumentSessionOperations> action) {
    sessionCreatedInternal.add(action);
  }

  public void removeSessionCreatedInternal(Action1<InMemoryDocumentSessionOperations> action) {
    sessionCreatedInternal.remove(action);
  }

  @Override
  public boolean getWasDisposed() {
    return wasDisposed;
  }

  protected void setWasDisposed(boolean wasDisposed) {
    this.wasDisposed = wasDisposed;
  }

  public abstract boolean hasJsonRequestFactory();

  @Override
  public Map<String, String> getSharedOperationsHeaders() {
    return sharedOperationsHeaders;
  }

  protected void setSharedOperationsHeaders(Map<String, String> sharedOperationsHeaders) {
    this.sharedOperationsHeaders = sharedOperationsHeaders;
  }

  @Override
  public void executeIndex(AbstractIndexCreationTask indexCreationTask) {
    indexCreationTask.execute(getDatabaseCommands(), getConventions());
  }
  @Override
  public void executeTransformer(AbstractTransformerCreationTask transformerCreationTask) {
    transformerCreationTask.execute(getDatabaseCommands(), getConventions());
  }

  @Override
  public DocumentConvention getConventions() {
    if (conventions == null) {
      conventions = new DocumentConvention();
    }
    return conventions;
  }

  public void setConventions(DocumentConvention conventions) {
    this.conventions = conventions;
  }

  public DocumentStore withConventions(DocumentConvention conventions) {
    this.conventions = conventions;
    return (DocumentStore) this;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }


  public boolean getEnlistInDistributedTransactions() {
    return getConventions().isEnlistInDistributedTransactions();
  }

  public void setEnlistInDistributedTransactions(boolean value) {
    getConventions().setEnlistInDistributedTransactions(value);
  }

  public UUID getResourceManagerId() {
    return resourceManagerId;
  }

  public void setResourceManagerId(UUID resourceManagerId) {
    this.resourceManagerId = resourceManagerId;
  }

  /**
   *  Gets the etag of the last document written by any session belonging to this
   *  document store
   */
  @Override
  public Etag getLastWrittenEtag() {
    return lastEtagHolder.getLastWrittenEtag();
  }

  //TODO: public abstract BulkInsertOperation BulkInsert(string database = null, BulkInsertOptions options = null);

  protected void ensureNotClosed() {
    if (wasDisposed) {
      throw new IllegalStateException("The document store has already been disposed and cannot be used");
    }
  }

  protected void assertInitialized() {
    if (!initialized) {
      throw new IllegalStateException("You cannot open a session or access the database commands before initializing the document store. Did you forget calling initialize()?");
    }
  }

  /**
   * Registers the conversion listener.
   * @param conversionListener
   * @return
   */
  public DocumentStoreBase registerListener(IDocumentConversionListener conversionListener) {
    listeners.getConversionListeners().add(conversionListener);
    return this;
  }

  /**
   * Registers the extended conversion listener.
   * @param conversionListener
   * @return
   */
  public DocumentStoreBase registerListener(IExtendedDocumentConversionListener conversionListener) {
    listeners.getExtendedConversionListeners().add(conversionListener);
    return this;
  }

  /**
   * Registers the query listener.
   * @param queryListener
   * @return
   */
  public DocumentStoreBase registerListener(IDocumentQueryListener queryListener) {
    listeners.getQueryListeners().add(queryListener);
    return this;
  }

  /**
   * Registers the store listener.
   * @param documentStoreListener
   * @return
   */
  public IDocumentStore registerListener(IDocumentStoreListener documentStoreListener) {
    listeners.getStoreListeners().add(documentStoreListener);
    return this;
  }

  /**
   * Registers the delete listener.
   * @param deleteListener
   * @return
   */
  public DocumentStoreBase registerListener(IDocumentDeleteListener deleteListener) {
    listeners.getDeleteListeners().add(deleteListener);
    return this;
  }

  /**
   * Registers the conflict listener.
   * @param conflictListener
   * @return
   */
  public DocumentStoreBase registerListener(IDocumentConflictListener conflictListener) {
    listeners.getConflictListeners().add(conflictListener);
    return this;
  }

  /**
   * Gets a read-only collection of the registered conversion listeners.
   */
  public ImmutableList<IDocumentConversionListener> getRegisteredConversionListeners() {
    return ImmutableList.copyOf(listeners.getConversionListeners());
  }

  /**
   * Gets a read-only collection of the registered query listeners.
   */
  public ImmutableList<IDocumentQueryListener> getRegisteredQueryListeners() {
    return ImmutableList.copyOf(listeners.getQueryListeners());
  }

  /**
   * Gets a read-only collection of the registered store listeners.
   */
  public ImmutableList<IDocumentStoreListener> getRegisteredStoreListeners() {
    return ImmutableList.copyOf(listeners.getStoreListeners());
  }

  /**
   * Gets a read-only collection of the registered delete listeners.
   */
  public ImmutableList<IDocumentDeleteListener> getRegisteredDeleteListeners() {
    return ImmutableList.copyOf(listeners.getDeleteListeners());
  }

  /**
   * Gets a read-only collection of the registered conflict listeners.
   */
  public ImmutableList<IDocumentConflictListener> getRegisteredConflictListeners() {
    return ImmutableList.copyOf(listeners.getConflictListeners());
  }

  protected void afterSessionCreated(InMemoryDocumentSessionOperations session) {
    EventHelper.invoke(sessionCreatedInternal, session);
  }

  public ILastEtagHolder getLastEtagHolder() {
    return lastEtagHolder;
  }

  public void setLastEtagHolder(ILastEtagHolder lastEtagHolder) {
    this.lastEtagHolder = lastEtagHolder;
  }

  public ITransactionRecoveryStorage getTransactionRecoveryStorage() {
    return transactionRecoveryStorage;
  }

  public void setTransactionRecoveryStorage(ITransactionRecoveryStorage transactionRecoveryStorage) {
    this.transactionRecoveryStorage = transactionRecoveryStorage;
  }

  /**
   * Get the profiling information for the given id
   * @param id
   * @return
   */
  public ProfilingInformation getProfilingInformationFor(UUID id) {
    return profilingContext.tryGet(id);
  }

  /**
   * Setup the context for aggressive caching.
   */
  @Override
  public AutoCloseable aggressivelyCache() {
    return aggressivelyCacheFor(24 * 3600 * 1000);
  }


}
