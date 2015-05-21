package net.ravendb.client.document;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.data.BatchResult;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.FacetQuery;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.QueryHeaderInformation;
import net.ravendb.abstractions.data.StreamResult;
import net.ravendb.abstractions.exceptions.ConcurrencyException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.IDocumentSessionImpl;
import net.ravendb.client.ISyncAdvancedSessionOperation;
import net.ravendb.client.LoadConfigurationFactory;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.RavenQueryHighlightings;
import net.ravendb.client.RavenQueryStatistics;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.IRavenQueryInspector;
import net.ravendb.client.connection.SerializationHelper;
import net.ravendb.client.document.batches.IEagerSessionOperations;
import net.ravendb.client.document.batches.ILazyOperation;
import net.ravendb.client.document.batches.ILazySessionOperations;
import net.ravendb.client.document.batches.LazyMultiLoadOperation;
import net.ravendb.client.document.sessionoperations.LoadOperation;
import net.ravendb.client.document.sessionoperations.LoadTransformerOperation;
import net.ravendb.client.document.sessionoperations.MultiLoadOperation;
import net.ravendb.client.document.sessionoperations.QueryOperation;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;
import net.ravendb.client.linq.IDocumentQueryGenerator;
import net.ravendb.client.linq.IRavenQueryProvider;
import net.ravendb.client.linq.IRavenQueryable;
import net.ravendb.client.linq.RavenQueryInspector;
import net.ravendb.client.linq.RavenQueryProvider;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Defaults;
import com.mysema.query.types.Expression;

/**
 * Implements Unit of Work for accessing the RavenDB server
 *
 */
public class DocumentSession extends InMemoryDocumentSessionOperations implements IDocumentSessionImpl, ISyncAdvancedSessionOperation, IDocumentQueryGenerator {

  private IDatabaseCommands databaseCommands;

  /**
   * Gets the database commands.
   */
  public IDatabaseCommands getDatabaseCommands() {
    return databaseCommands;
  }

  /**
   * Access the lazy operations
   */
  @Override
  public ILazySessionOperations lazily() {
    return new LazySessionOperations(this);
  }

  /**
   * Access the eager operations
   */
  @Override
  public IEagerSessionOperations eagerly() {
    return this;
  }

  /**
   * Initializes a new instance of the {@link DocumentSession} class.
   * @param dbName
   * @param documentStore
   * @param listeners
   * @param id
   * @param databaseCommands
   */
  public DocumentSession(String dbName, DocumentStore documentStore,
      DocumentSessionListeners listeners,
      UUID id,
      IDatabaseCommands databaseCommands) {
    super(dbName, documentStore, listeners, id);
    this.databaseCommands = databaseCommands;
  }

  /**
   * Get the accessor for advanced operations
   *
   * Note: Those operations are rarely needed, and have been moved to a separate
   * property to avoid cluttering the API
   */
  @Override
  public ISyncAdvancedSessionOperation advanced() {
    return this;
  }

  protected class DisableAllCachingCallback implements Function0<CleanCloseable> {
    @SuppressWarnings("synthetic-access")
    @Override
    public CleanCloseable apply() {
      return databaseCommands.disableAllCaching();
    }
  }

  /**
   * Loads the specified entity with the specified id.
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T load(Class<T> clazz, String id) {
    if (id == null) {
      throw new IllegalArgumentException("The document id cannot be null");
    }
    if (isDeleted(id)) {
      return Defaults.defaultValue(clazz);
    }
    Object existingEntity;
    if (entitiesByKey.containsKey(id)) {
      existingEntity = entitiesByKey.get(id);
      return (T) existingEntity;
    }

    if (includedDocumentsByKey.containsKey(id)) {
      JsonDocument value = includedDocumentsByKey.get(id);
      includedDocumentsByKey.remove(id);
      return (T) trackEntity(clazz, value);
    }

    incrementRequestCount();

    LoadOperation loadOperation = new LoadOperation(this, new DisableAllCachingCallback(), id);
    boolean retry;
    do {
      loadOperation.logOperation();
      try (CleanCloseable close = loadOperation.enterLoadContext()) {
        retry = loadOperation.setResult(databaseCommands.get(id));
      } catch (ConflictException e) {
        throw e;
      }
    } while (retry);
    return loadOperation.complete(clazz);
  }


  /**
   * Loads the specified entities with the specified ids.
   */
  @Override
  public <T> T[] load(Class<T> clazz, Collection<String> ids) {
    return ((IDocumentSessionImpl) this).loadInternal(clazz, ids.toArray(new String[0]));
  }

  @Override
  public <T> T[] load(Class<T> clazz, String... ids) {
    return ((IDocumentSessionImpl) this).loadInternal(clazz, ids);
  }

  @SuppressWarnings("boxing")
  @Override
  public <T> T load(Class<T> clazz, Number id) {
    String documentKey = getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(id, clazz, false);
    return load(clazz, documentKey);
  }

  @SuppressWarnings("boxing")
  @Override
  public <T> T load(Class<T> clazz, UUID id) {
    String documentKey = getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(id, clazz, false);
    return load(clazz, documentKey);
  }

  @SuppressWarnings("boxing")
  @Override
  public <T> T[] load(Class<T> clazz, Number... ids) {
    List<String> documentKeys = new ArrayList<>();
    for (Number id: ids) {
      documentKeys.add(getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(id, clazz, false));
    }
    return load(clazz, documentKeys.toArray(new String[0]));
  }

  @SuppressWarnings("boxing")
  @Override
  public <T> T[] load(Class<T> clazz, UUID... ids) {
    List<String> documentKeys = new ArrayList<>();
    for (UUID id: ids) {
      documentKeys.add(getConventions().getFindFullDocumentKeyFromNonStringIdentifier().find(id, clazz, false));
    }
    return load(clazz, documentKeys.toArray(new String[0]));
  }


  private <T> T[] loadUsingTransformerInternal(Class<T> clazz, String[] ids, String transformer) {
    return loadUsingTransformerInternal(clazz, ids, transformer, null);
  }

  @SuppressWarnings("unchecked")
  private <T> T[] loadUsingTransformerInternal(Class<T> clazz, String[] ids, String transformer, Map<String, RavenJToken> transformerParameters) {
    if (transformer == null) {
      throw new NullArgumentException("transformer");
    }
    if (ids.length == 0) {
      return (T[]) Array.newInstance(clazz, 0);
    }

    incrementRequestCount();

    MultiLoadResult multiLoadResult = getDatabaseCommands().get(ids, new String[] { }, transformer, transformerParameters);
    return new LoadTransformerOperation(this, transformer, ids).complete(clazz, multiLoadResult);
  }



  @SuppressWarnings({"null", "unchecked"})
  @Override
  public <T> T[] loadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes) {
    if (ids.length == 0) {
      return (T[]) Array.newInstance(clazz, 0);
    }

    if (checkIfIdAlreadyIncluded(ids, includes)) {
      T[] result = (T[]) Array.newInstance(clazz, ids.length);
      for (int i = 0; i < ids.length; i++) {
        result[i] = load(clazz, ids[i]);
      }
      return result;
    }

    List<String> includePaths = null;
    if (includes != null) {
      includePaths = new ArrayList<>();
      for (Tuple<String, Class<?>> item: includes) {
        includePaths.add(item.getItem1());
      }
    }

    incrementRequestCount();

    MultiLoadOperation multiLoadOperation = new MultiLoadOperation(this, new DisableAllCachingCallback(), ids, includes);
    MultiLoadResult multiLoadResult = null;
    do {
      multiLoadOperation.logOperation();
      try (CleanCloseable context = multiLoadOperation.enterMultiLoadContext()) {
        multiLoadResult = databaseCommands.get(ids, includePaths.toArray(new String[0]));
      }
    } while (multiLoadOperation.setResult(multiLoadResult));

    return multiLoadOperation.complete(clazz);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] loadInternal(Class<T> clazz, String[] ids) {
    if (ids.length == 0) {
      return (T[]) Array.newInstance(clazz, 0);
    }

    // only load documents that aren't already cached
    Set<String> idsOfNotExistingObjects = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    for (String id: ids) {
      if (!isLoaded(id) && !isDeleted(id)) {
        idsOfNotExistingObjects.add(id);
      }
    }

    if (idsOfNotExistingObjects.size() > 0) {
      incrementRequestCount();
      MultiLoadOperation multiLoadOperation = new MultiLoadOperation(this, new DisableAllCachingCallback(), idsOfNotExistingObjects.toArray(new String[0]), null);
      MultiLoadResult multiLoadResult = null;
      do {
        multiLoadOperation.logOperation();
        try (CleanCloseable context = multiLoadOperation.enterMultiLoadContext()) {
          multiLoadResult = databaseCommands.get(idsOfNotExistingObjects.toArray(new String[0]), null);
        }
      } while (multiLoadOperation.setResult(multiLoadResult));

      multiLoadOperation.complete(clazz);
    }

    List<Object> result = new ArrayList<>();
    for (String id: ids) {
      result.add(load(clazz, id));
    }
    return result.toArray((T[]) Array.newInstance(clazz, 0));
  }

  /**
   * Queries the specified index.
   * @param clazz
   * @param indexName
   */
  @Override
  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName) {
    return query(clazz, indexName, false);
  }

  /**
   * Queries the specified index.
   * @param clazz The result of the query
   * @param indexName Name of the index.
   * @param isMapReduce Whatever we are querying a map/reduce index (modify how we treat identifier properties)
   */
  @Override
  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName, boolean isMapReduce) {
    RavenQueryStatistics ravenQueryStatistics = new RavenQueryStatistics();
    RavenQueryHighlightings highlightings = new RavenQueryHighlightings();
    RavenQueryProvider<T> ravenQueryProvider = new RavenQueryProvider<>(clazz, this, indexName, ravenQueryStatistics, highlightings, getDatabaseCommands(), isMapReduce);
    RavenQueryInspector<T> inspector = new RavenQueryInspector<>();
    inspector.init(clazz, ravenQueryProvider, ravenQueryStatistics, highlightings, indexName, null, this, getDatabaseCommands(), isMapReduce);
    return inspector;
  }

  /**
   * Queries the index specified by tIndexCreator using Linq.
   * @param clazz The result of the query
   * @param tIndexCreator The type of the index creator
   */
  @Override
  public <T> IRavenQueryable<T> query(Class<T> clazz, Class<? extends AbstractIndexCreationTask> tIndexCreator) {
    try {
      AbstractIndexCreationTask indexCreator = tIndexCreator.newInstance();
      return query(clazz, indexCreator.getIndexName(), indexCreator.isMapReduce());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(tIndexCreator.getName() + " does not have argumentless constructor.");
    }
  }

  /**
   * Refreshes the specified entity from Raven server.
   */
  @Override
  public <T> void refresh(T entity) {
    DocumentMetadata value;
    if (!entitiesAndMetadata.containsKey(entity)) {
      throw new IllegalStateException("Cannot refresh a transient instance");
    }
    value = entitiesAndMetadata.get(entity);
    incrementRequestCount();
    JsonDocument jsonDocument = databaseCommands.get(value.getKey());
    refreshInternal(entity, jsonDocument, value);
  }


  /**
   * Get the json document by key from the store
   */
  @Override
  protected JsonDocument getJsonDocument(String documentKey) {
    JsonDocument jsonDocument = databaseCommands.get(documentKey);
    if (jsonDocument == null) {
      throw new IllegalStateException("Document '" + documentKey + "' no longer exists and was probably deleted");
    }
    return jsonDocument;
  }

  @Override
  protected String generateKey(Object entity) {
    return getConventions().generateDocumentKey(dbName, databaseCommands, entity);
  }

  /**
   * Begin a load while including the specified path
   */
  @Override
  public ILoaderWithInclude include(String path) {
    return new MultiLoaderWithInclude(this).include(path);
  }

  /**
   * Begin a load while including the specified path
   * @param path
   */
  @Override
  public ILoaderWithInclude include(Expression<?> path) {
    return new MultiLoaderWithInclude(this).include(path);
  }

  /**
   * Begin a load while including the specified path
   * @param path
   */
  @Override
  public ILoaderWithInclude include(Class<?> targetClass, Expression<?> path) {
    return new MultiLoaderWithInclude(this).include(targetClass, path);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult load(
    Class<TTransformer> tranformerClass, Class<TResult> clazz, String id) {
    return load(tranformerClass, clazz, id, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, String id, LoadConfigurationFactory configureFactory) {
    try {
      String transformer = tranformerClass.newInstance().getTransformerName();
      RavenLoadConfiguration configuration = new RavenLoadConfiguration();
      if (configureFactory != null) {
        configureFactory.configure(configuration);
      }
      TResult[] loadResult = loadUsingTransformerInternal(clazz, new String[] { id} , transformer, configuration.getTransformerParameters());
      if (loadResult != null && loadResult.length > 0 ) {
        return loadResult[0];
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, String... ids) {
    try {
      String transformer = tranformerClass.newInstance().getTransformerName();
      return loadUsingTransformerInternal(clazz, ids , transformer);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, List<String> ids, LoadConfigurationFactory configureFactory) {
    try {
      String transformer = tranformerClass.newInstance().getTransformerName();
      RavenLoadConfiguration configuration = new RavenLoadConfiguration();
      if (configureFactory != null) {
        configureFactory.configure(configuration);
      }
      return loadUsingTransformerInternal(clazz, ids.toArray(new String[0]) , transformer, configuration.getTransformerParameters());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <TResult> TResult load(Class<TResult> clazz, String transformer, String id) {
    return load(clazz, transformer, id, null);
  }

  @Override
  public <TResult> TResult load(Class<TResult> clazz, String transformer, String id, LoadConfigurationFactory configure) {
    RavenLoadConfiguration configuration = new RavenLoadConfiguration();
    if (configure != null){
      configure.configure(configuration);
    }

    TResult[] loadResult = loadUsingTransformerInternal(clazz, new String[] { id }, transformer, configuration.getTransformerParameters());
    if (loadResult != null && loadResult.length > 0 ) {
      return loadResult[0];
    }
    return null;
  }

  @Override
  public <TResult> TResult[] load(Class<TResult> clazz, String transformer, Collection<String> ids) {
    return load(clazz, transformer, ids, null);
  }

  @Override
  public <TResult> TResult[] load(Class<TResult> clazz, String transformer, Collection<String> ids,
    LoadConfigurationFactory configure) {
    RavenLoadConfiguration configuration = new RavenLoadConfiguration();
    if (configure != null){
      configure.configure(configuration);
    }

    return loadUsingTransformerInternal(clazz, ids.toArray(new String[0]), transformer, configuration.getTransformerParameters());
  }

  /**
   * Gets the document URL for the specified entity.
   */
  @Override
  public String getDocumentUrl(Object entity) {
    if (!entitiesAndMetadata.containsKey(entity)) {
      throw new IllegalStateException("Could not figure out identifier for transient instance");
    }
    DocumentMetadata value = entitiesAndMetadata.get(entity);
    return databaseCommands.urlFor(value.getKey());
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query) {
    Reference<QueryHeaderInformation> _ = new Reference<>();
    return stream(query, _);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query, Reference<QueryHeaderInformation> queryHeaderInformationRef) {
    IRavenQueryProvider queryProvider = (IRavenQueryProvider)query.getProvider();
    IDocumentQuery<T> docQuery = (IDocumentQuery<T>) queryProvider.toDocumentQuery(query.getElementType(), query.getExpression());
    return stream(docQuery, queryHeaderInformationRef);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query) {
    Reference<QueryHeaderInformation> _ = new Reference<>();
    return stream(query, _);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query, Reference<QueryHeaderInformation> queryHeaderInformation) {
    IRavenQueryInspector ravenQueryInspector = (IRavenQueryInspector) query;
    IndexQuery indexQuery = ravenQueryInspector.getIndexQuery();

    boolean waitForNonStaleResultsWasSetGloably = advanced().getDocumentStore().getConventions().getDefaultQueryingConsistency() == ConsistencyOptions.ALWAYS_WAIT_FOR_NON_STALE_RESULTS_AS_OF_LAST_WRITE;

    if (!waitForNonStaleResultsWasSetGloably && (indexQuery.isWaitForNonStaleResults() || indexQuery.isWaitForNonStaleResultsAsOfNow())) {
      throw new IllegalArgumentException(
          "Since stream() does not wait for indexing (by design), streaming query with WaitForNonStaleResults is not supported.");
    }
    incrementRequestCount();

    CloseableIterator<RavenJObject> iterator = databaseCommands.streamQuery(ravenQueryInspector.getIndexQueried(), indexQuery, queryHeaderInformation);
    return new StreamIterator<>(query, iterator);
  }


  private static class StreamIterator<T> implements CloseableIterator<StreamResult<T>> {

    private CloseableIterator<RavenJObject> innerIterator;
    private DocumentQuery<T> query;
    private QueryOperation queryOperation;

    public StreamIterator(IDocumentQuery<T> query, CloseableIterator<RavenJObject> innerIterator) {
      super();
      this.innerIterator = innerIterator;
      this.query = (DocumentQuery<T>) query;
      queryOperation = ((DocumentQuery<T>)query).initializeQueryOperation();
      queryOperation.setDisableEntitiesTracking(true);
    }

    @Override
    public boolean hasNext() {
      return innerIterator.hasNext();
    }

    @Override
    public void close() {
      innerIterator.close();
    }

    @Override
    public StreamResult<T> next() {
      RavenJObject nextValue = innerIterator.next();
      query.invokeAfterStreamExecuted(new Reference<>(nextValue));
      RavenJObject meta = nextValue.value(RavenJObject.class, Constants.METADATA);

      String key = null;
      Etag etag = null;
      if (meta != null) {
        key = meta.value(String.class, "@id");
        if (key == null) {
          key = meta.value(String.class, Constants.DOCUMENT_ID_FIELD_NAME);
        }
        if (key == null) {
          key = nextValue.value(String.class, Constants.DOCUMENT_ID_FIELD_NAME);
        }

        String value = meta.value(String.class, "@etag");
        if (value != null) {
          etag = Etag.parse(value);
        }
      }

      StreamResult<T> streamResult = new StreamResult<>();
      streamResult.setDocument(queryOperation.deserialize(query.getElementType(), nextValue));
      streamResult.setEtag(etag);
      streamResult.setKey(key);
      streamResult.setMetadata(meta);
      return streamResult;
    }

    @Override
    public void remove() {
      throw new IllegalStateException("Not implemented!");
    }

  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass) {
    return stream(entityClass, null, null, null, 0, Integer.MAX_VALUE);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag) {
    return stream(entityClass, fromEtag, null, null, 0, Integer.MAX_VALUE);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith) {
    return stream(entityClass, fromEtag, startsWith, null, 0, Integer.MAX_VALUE);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches) {
    return stream(entityClass, fromEtag, startsWith, matches, 0, Integer.MAX_VALUE);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start) {
    return stream(entityClass, fromEtag, startsWith, matches, start, Integer.MAX_VALUE);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize) {
    return stream(entityClass, fromEtag, startsWith, matches, start, pageSize, null);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize, RavenPagingInformation pagingInformation) {
    return stream(entityClass, fromEtag, startsWith, matches, start, pageSize, pagingInformation, null);
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize, RavenPagingInformation pagingInformation, String skipAfter) {
    incrementRequestCount();
    CloseableIterator<RavenJObject> iterator = databaseCommands.streamDocs(fromEtag, startsWith, matches, start, pageSize, null, pagingInformation, skipAfter);
    return new SimpleSteamIterator<>(iterator, entityClass);
  }

  @Override
  public FacetResults[] multiFacetedSearch(FacetQuery...facetQueries) {
    incrementRequestCount();
    return databaseCommands.getMultiFacets(facetQueries);
  }

  private class SimpleSteamIterator<T> implements CloseableIterator<StreamResult<T>> {
    private CloseableIterator<RavenJObject> innerIterator;
    private Class<T> entityClass;
    private boolean closed = false;

    public SimpleSteamIterator(CloseableIterator<RavenJObject> innerIterator, Class<T> entityClass) {
      super();
      this.innerIterator = innerIterator;
      this.entityClass = entityClass;
    }

    @Override
    public boolean hasNext() {
      return innerIterator.hasNext();
    }

    @Override
    public void close() {
      closed = true;
      innerIterator.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public StreamResult<T> next() {
      if (closed) {
        throw new IllegalStateException("Stream is closed");
      }
      RavenJObject next = innerIterator.next();
      JsonDocument document = SerializationHelper.ravenJObjectToJsonDocument(next);
      StreamResult<T> streamResult = new StreamResult<>();
      streamResult.setDocument((T) convertToEntity(entityClass, document.getKey(), document.getDataAsJson(), document.getMetadata()));
      streamResult.setEtag(document.getEtag());
      streamResult.setKey(document.getKey());
      streamResult.setMetadata(document.getMetadata());
      return streamResult;
    }

    @Override
    public void remove() {
      throw new IllegalStateException("Not implemented!");
    }
  }

  /**
   * Saves all the changes to the Raven server.
   */
  @Override
  public void saveChanges() {
    try (CleanCloseable scope = entityToJson.entitiesToJsonCachingScope()) {
      SaveChangesData data = prepareForSaveChanges();

      if (data.getCommands().size() == 0) {
        return ; // nothing to do here
      }

      incrementRequestCount();
      logBatch(data);

      BatchResult[] batchResults = getDatabaseCommands().batch(data.getCommands());

      if (batchResults == null) {
        throw new IllegalStateException("Cannot call Save Changes after the document store was disposed.");
      }

      updateBatchResults(Arrays.asList(batchResults), data);
    } catch (ConcurrencyException e) {
      throw e;
    }
  }

  /**
   * Queries the index specified by <typeparamref name="TIndexCreator"/> using lucene syntax.
   * @param clazz The result of the query
   * @param indexClazz The type of the index creator.
   */
  @Override
  public <T, TIndexCreator extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<TIndexCreator> indexClazz) {
    try {
      TIndexCreator index = indexClazz.newInstance();
      return documentQuery(clazz, index.getIndexName(), index.isMapReduce());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(indexClazz.getName() + " does not have argumentless constructor.");
    }
  }

  @Override
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName) {
    return documentQuery(clazz, indexName, false);
  }

  /**
   * Query the specified index using Lucene syntax
   * @param clazz
   * @param indexName Name of the index.
   * @param isMapReduce
   */
  @Override
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, boolean isMapReduce) {
    return new DocumentQuery<>(clazz, this, getDatabaseCommands(), indexName, null, null, theListeners.getQueryListeners(), isMapReduce);
  }

  /**
   * Query RavenDB dynamically using
   * @param clazz
   */
  @Override
  public <T> IRavenQueryable<T> query(Class<T> clazz) {
    String indexName = createDynamicIndexName(clazz);
    return query(clazz, indexName);
  }


  /**
   * Dynamically query RavenDB using Lucene syntax
   */
  @Override
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz) {
    String indexName = createDynamicIndexName(clazz);
    return advanced().documentQuery(clazz, indexName);
  }

  @Override
  @SuppressWarnings("unused")
  public <S> RavenQueryInspector<S> createRavenQueryInspector() {
    return new RavenQueryInspector<S>();
  }

  @SuppressWarnings("unchecked")
  public <T> Lazy<T> addLazyOperation(final ILazyOperation operation, final Action1<T> onEval) {
    pendingLazyOperations.add(operation);
    Lazy<T> lazyValue = new Lazy<>(new Function0<T>() {
      @Override
      public T apply() {
        executeAllPendingLazyOperations();
        return (T) operation.getResult();
      }
    });

    if (onEval != null) {
      onEvaluateLazy.put(operation, new Action1<Object>() {
        @Override
        public void apply(Object theResult) {
          onEval.apply((T)theResult);
        }
      });
    }
    return lazyValue;
  }

  public Lazy<Integer> addLazyCountOperation(final ILazyOperation operation)
  {
      pendingLazyOperations.add(operation);

      Lazy<Integer> lazyValue = new Lazy<>(new Function0<Integer>() {
        @SuppressWarnings("boxing")
        @Override
        public Integer apply() {
          executeAllPendingLazyOperations();
          return operation.getQueryResult().getTotalResults();
        }
      });
      return lazyValue;
  }


  /**
   * Register to lazily load documents and include
   */
  @Override
  public <T> Lazy<T[]> lazyLoadInternal(final Class<T> clazz, final String[] ids, Tuple<String, Class<?>>[] includes, Action1<T[]> onEval) {
    if (checkIfIdAlreadyIncluded(ids, includes)) {
      return new Lazy<>(new Function0<T[]>() {
        @SuppressWarnings("unchecked")
        @Override
        public T[] apply() {
          T[] result = (T[]) Array.newInstance(clazz, ids.length);
          for (int i = 0; i < ids.length; i++) {
            result[i] = load(clazz, ids[i]);
          }
          return result;
        }
      });
    }
    MultiLoadOperation multiLoadOperation = new MultiLoadOperation(this, new DisableAllCachingCallback(), ids, includes);
    LazyMultiLoadOperation<T> lazyOp = new LazyMultiLoadOperation<>(clazz, multiLoadOperation, ids, includes, null);
    return addLazyOperation(lazyOp, onEval);
  }

  @SuppressWarnings("boxing")
  @Override
  public ResponseTimeInformation executeAllPendingLazyOperations() {
    if (pendingLazyOperations.size() == 0)
      return new ResponseTimeInformation();

    try {
      incrementRequestCount();

      ResponseTimeInformation responseTimeDuration = new ResponseTimeInformation();
      long time1 = new Date().getTime();
      try {
        while (executeLazyOperationsSingleStep(responseTimeDuration)) {
          Thread.sleep(100);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      for (ILazyOperation pendingLazyOperation : pendingLazyOperations) {
        if (onEvaluateLazy.containsKey(pendingLazyOperation)) {
          onEvaluateLazy.get(pendingLazyOperation).apply(pendingLazyOperation.getResult());
        }
      }

      long time2 = new Date().getTime();
      responseTimeDuration.setTotalClientDuration(time2 - time1);

      return responseTimeDuration;
    } finally {
      pendingLazyOperations.clear();
    }
  }

  @SuppressWarnings("boxing")
  private boolean executeLazyOperationsSingleStep(ResponseTimeInformation responseTimeInformation) {

    List<CleanCloseable> disposables = new ArrayList<>();
    for (ILazyOperation lazyOp: pendingLazyOperations) {
      CleanCloseable context = lazyOp.enterContext();
      if (context != null) {
        disposables.add(context);
      }
    }

    try {
      List<GetRequest> requests = new ArrayList<>();
      for (ILazyOperation lazyOp: pendingLazyOperations) {
        requests.add(lazyOp.createRequest());
      }
      GetResponse[] responses = databaseCommands.multiGet(requests.toArray(new GetRequest[0]));
      for (int i = 0; i < pendingLazyOperations.size(); i++) {

        String tempRequestTime = responses[0].getHeaders().get("Temp-Request-Time");
        Long parsedValue = 0L;
        try {
          parsedValue = Long.parseLong(tempRequestTime);
        } catch (NumberFormatException e)  {
          // ignore
        }
        ResponseTimeInformation.ResponseTimeItem responseTimeItem = new ResponseTimeInformation.ResponseTimeItem();
        responseTimeItem.setUrl(requests.get(i).getUrlAndQuery());
        responseTimeItem.setDuration(parsedValue);
        responseTimeInformation.getDurationBreakdown().add(responseTimeItem);

        if (responses[i].isRequestHasErrors()) {
          throw new IllegalStateException("Got an error from server, status code: " + responses[i].getStatus()  + "\n" + responses[i].getResult());
        }
        pendingLazyOperations.get(i).handleResponse(responses[i]);
        if (pendingLazyOperations.get(i).isRequiresRetry()) {
          return true;
        }
      }
      return false;
    } finally {
      for (CleanCloseable closable: disposables) {
        closable.close();
      }
    }
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix) {
    return loadStartingWith(clazz, keyPrefix, null, 0, 25);
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches) {
    return loadStartingWith(clazz, keyPrefix, matches, 0, 25);
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start) {
    return loadStartingWith(clazz, keyPrefix, matches, start, 25);
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize) {
    return loadStartingWith(clazz, keyPrefix, matches, start, 25, null, null);
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude) {
    return loadStartingWith(clazz, keyPrefix, matches, start, pageSize, exclude, null);
  }

  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation) {
    return loadStartingWith(clazz, keyPrefix, matches, start, pageSize, exclude, pagingInformation, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation, String skipAfter) {
    incrementRequestCount();
    List<JsonDocument> results = getDatabaseCommands().startsWith(keyPrefix, matches, start, pageSize, false, exclude, pagingInformation, null, null, skipAfter);
    for (JsonDocument doc: results) {
      trackEntity(clazz, doc);
    }
    return results.toArray((T[])Array.newInstance(clazz, 0));
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, null, 0, 25, null, null, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, 0, 25, null, null, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, start, 25, null, null, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, start, pageSize, null, null, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, start, pageSize, exclude, null, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, start, pageSize, exclude, pagingInformation, null, null);
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation, LoadConfigurationFactory configure) {
    return loadStartingWith(clazz, transformerClass, keyPrefix, matches, start, pageSize, exclude, pagingInformation, configure, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation, LoadConfigurationFactory configure, String skipAfter) {

    incrementRequestCount();

    try {
      String transformer = transformerClass.newInstance().getTransformerName();
      RavenLoadConfiguration configuration = new RavenLoadConfiguration();
      if (configure != null) {
        configure.configure(configuration);
      }

      List<JsonDocument> documents = getDatabaseCommands().startsWith(keyPrefix, matches, start, pageSize, false, exclude, pagingInformation, transformer, configuration.getTransformerParameters(), skipAfter);
      List<TResult> result = new ArrayList<>(documents.size());
      for (JsonDocument document : documents) {
        result.add((TResult)trackEntity(clazz, document));
      }

      return result.toArray((TResult[])Array.newInstance(clazz, 0));
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    String documentId) {
    try {
      AbstractIndexCreationTask indexCreatorInstance = indexCreator.newInstance();
      MoreLikeThisQuery query = new MoreLikeThisQuery();
      query.setDocumentId(documentId);
      return moreLikeThis(entityClass, indexCreatorInstance.getIndexName(), null, query);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Unable to initialise index:" + indexCreator, e);
    }
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    MoreLikeThisQuery parameters) {
    try {
      AbstractIndexCreationTask indexCreatorInstance = indexCreator.newInstance();
      return moreLikeThis(entityClass, indexCreatorInstance.getIndexName(), null, parameters);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Unable to initialise index:" + indexCreator, e);
    }
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String documentId) {
    MoreLikeThisQuery query = new MoreLikeThisQuery();
    query.setDocumentId(documentId);
    return moreLikeThis(entityClass, index, null, query);
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    Class<? extends AbstractTransformerCreationTask> transformerClass, String documentId) {
    try {
      AbstractIndexCreationTask indexCreatorInstance = indexCreator.newInstance();
      AbstractTransformerCreationTask transformerInstance = transformerClass.newInstance();
      MoreLikeThisQuery query = new MoreLikeThisQuery();
      query.setDocumentId(documentId);
      return moreLikeThis(entityClass, indexCreatorInstance.getIndexName(), transformerInstance.getTransformerName(), query);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Unable to initialise index:" + indexCreator, e);
    }
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    Class<? extends AbstractTransformerCreationTask> transformerClass, MoreLikeThisQuery parameters) {
    try {
      AbstractIndexCreationTask indexCreatorInstance = indexCreator.newInstance();
      AbstractTransformerCreationTask transformerInstance = transformerClass.newInstance();
      return moreLikeThis(entityClass, indexCreatorInstance.getIndexName(), transformerInstance.getTransformerName(), parameters);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException("Unable to initialise index:" + indexCreator, e);
    }
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, String documentId) {
    MoreLikeThisQuery query = new MoreLikeThisQuery();
    query.setDocumentId(documentId);
    return moreLikeThis(entityClass, index, transformer, query);
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, MoreLikeThisQuery parameters) {
    if (StringUtils.isEmpty(index)) {
      throw new IllegalArgumentException("Index name cannot be null or empty");
    }

    parameters.setIndexName(index);
    parameters.setResultsTransformer(transformer);

    // /morelikethis/(index-name)/(ravendb-document-id)?fields=(fields)
    IDatabaseCommands cmd = getDatabaseCommands();

    incrementRequestCount();

    MultiLoadOperation multiLoadOperation = new MultiLoadOperation(this, new DisableAllCachingCallback(), null, null);
    MultiLoadResult multiLoadResult = null;
    do {
      multiLoadOperation.logOperation();
      try (CleanCloseable multiLoadContext = multiLoadOperation.enterMultiLoadContext()) {
        multiLoadResult = cmd.moreLikeThis(parameters);
      }
    } while (multiLoadOperation.setResult(multiLoadResult));

    return multiLoadOperation.complete(entityClass);
  }
}
