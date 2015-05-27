package net.ravendb.client.shard;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.data.BatchResult;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.FacetQuery;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.QueryHeaderInformation;
import net.ravendb.abstractions.data.StreamResult;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.IDocumentSessionImpl;
import net.ravendb.client.ISyncAdvancedSessionOperation;
import net.ravendb.client.LoadConfigurationFactory;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentMetadata;
import net.ravendb.client.document.DocumentSessionListeners;
import net.ravendb.client.document.ILoaderWithInclude;
import net.ravendb.client.document.LazyShardSessionOperations;
import net.ravendb.client.document.MultiLoaderWithInclude;
import net.ravendb.client.document.RavenLoadConfiguration;
import net.ravendb.client.document.ResponseTimeInformation;
import net.ravendb.client.document.SaveChangesData;
import net.ravendb.client.document.batches.IEagerSessionOperations;
import net.ravendb.client.document.batches.ILazyOperation;
import net.ravendb.client.document.batches.ILazySessionOperations;
import net.ravendb.client.document.batches.LazyMultiLoadOperation;
import net.ravendb.client.document.sessionoperations.LoadOperation;
import net.ravendb.client.document.sessionoperations.LoadTransformerOperation;
import net.ravendb.client.document.sessionoperations.MultiLoadOperation;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;
import net.ravendb.client.linq.IDocumentQueryGenerator;
import net.ravendb.client.linq.IRavenQueryable;
import net.ravendb.client.linq.RavenQueryInspector;

import com.google.common.base.Defaults;
import com.google.common.base.Objects;
import com.mysema.query.types.Expression;


public class ShardedDocumentSession extends BaseShardedDocumentSession<IDatabaseCommands>
implements IDocumentQueryGenerator, IDocumentSessionImpl, ISyncAdvancedSessionOperation {

  /**
   * Initializes a new instance of the ShardedDocumentSession class.
   */
  public ShardedDocumentSession(String dbName, ShardedDocumentStore documentStore, DocumentSessionListeners listeners, UUID id,
    ShardStrategy shardStrategy, Map<String, IDatabaseCommands> shardDbCommands) {
    super(dbName, documentStore, listeners, id, shardStrategy, shardDbCommands);
  }

  @Override
  protected JsonDocument getJsonDocument(final String documentKey) {
    ShardRequestData shardRequestData = new ShardRequestData();
    shardRequestData.setEntityType(Object.class);
    shardRequestData.setKeys(Arrays.asList(documentKey));

    List<IDatabaseCommands> dbCommands = getCommandsToOperateOn(shardRequestData);
    JsonDocument[] documents = getShardStrategy().getShardAccessStrategy().apply(JsonDocument.class, dbCommands, shardRequestData, new Function2<IDatabaseCommands, Integer, JsonDocument>() {
      @Override
      public JsonDocument apply(IDatabaseCommands commands, Integer i) {
        return commands.get(documentKey);
      }
    });
    for (JsonDocument document: documents) {
      if (document != null) {
        return document;
      }
    }
    throw new IllegalStateException("Document '" + documentKey + "' no longer exists and was probably deleted.");
  }

  @Override
  protected String generateKey(Object entity) {
    String shardId = getShardStrategy().getShardResolutionStrategy().metadataShardIdFor(entity);
    IDatabaseCommands value = shardDbCommands.get(shardId);
    if (value == null) {
      throw new IllegalStateException("Could not find shard: " + shardId);
    }
    return getConventions().generateDocumentKey(dbName, value, entity);
  }

  /**
   * Access the lazy operations
   */
  @Override
  public ILazySessionOperations lazily() {
    return new LazyShardSessionOperations(this);
  }

  /**
   * Access the eager operations
   */
  @Override
  public IEagerSessionOperations eagerly() {
    return this;
  }

  @Override
  public ISyncAdvancedSessionOperation advanced() {
    return this;
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult load(
    Class<TTransformer> tranformerClass, Class<TResult> clazz, String id) {
    try {
      String transformer = tranformerClass.newInstance().getTransformerName();
      TResult[] loadResult = loadInternal(clazz, new String[] { id}, null, transformer);
      if (loadResult != null && loadResult.length > 0 ) {
        return loadResult[0];
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
      TResult[] loadResult = loadInternal(clazz, new String[] { id }, null, transformer, configuration.getTransformerParameters());
      if (loadResult != null && loadResult.length > 0 ) {
        return loadResult[0];
      }
      return null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T load(final Class<T> clazz, final String id) {
    Object existingEntity;
    if (entitiesByKey.containsKey(id)) {
      existingEntity = entitiesByKey.get(id);
      return (T) existingEntity;
    }

    incrementRequestCount();

    ShardRequestData shardRequestData = new ShardRequestData(Arrays.asList(id), clazz);
    List<IDatabaseCommands> dbCommands = getCommandsToOperateOn(shardRequestData);
    T[] results = getShardStrategy().getShardAccessStrategy().apply(clazz, dbCommands, shardRequestData, new Function2<IDatabaseCommands, Integer, T>() {
      @Override
      public T apply(final IDatabaseCommands commands, Integer i) {
        LoadOperation loadOperation = new LoadOperation(ShardedDocumentSession.this, new Function0<CleanCloseable>() {
          @Override
          public CleanCloseable apply() {
            return commands.disableAllCaching();
          }
        }, id);
        boolean retry;
        do {
          loadOperation.logOperation();
          try (CleanCloseable close = loadOperation.enterLoadContext()) {
            retry = loadOperation.setResult(commands.get(id));
          } catch (ConflictException e) {
            throw e;
          }
        } while (retry);
        return loadOperation.complete(clazz);
      }
    });

    List<T> shardsContainThisDocument = new ArrayList<>();
    for (T result: results) {
      if (!Objects.equal(result, Defaults.defaultValue(clazz))) {
        shardsContainThisDocument.add(result);
      }
    }
    if (shardsContainThisDocument.size() > 1) {
      throw new IllegalStateException("Found document with id: " + id + " on more than a single shard, which is not allowed. Document keys have to be unique cluster-wide.");
    }
    return shardsContainThisDocument.size() > 0 ? shardsContainThisDocument.get(0) : null;
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

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(Class<TTransformer> tranformerClass,
    Class<TResult> clazz, List<String> ids, LoadConfigurationFactory configureFactory) {
    try {
      String transformer = tranformerClass.newInstance().getTransformerName();
      RavenLoadConfiguration configuration = new RavenLoadConfiguration();
      if (configureFactory != null) {
        configureFactory.configure(configuration);
      }
      return loadInternal(clazz, ids.toArray(new String[0]), null, transformer, configuration.getTransformerParameters());
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

    TResult[] loadResult = loadInternal(clazz, new String[] { id }, null, transformer, configuration.getTransformerParameters());
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

    return loadInternal(clazz, ids.toArray(new String[0]), null, transformer, configuration.getTransformerParameters());
  }

  @SuppressWarnings("rawtypes")
  private <T> T[] loadInternal(Class<T> clazz, String[] ids, List<Tuple<String, Class>> includes, String transformer) {
    return loadInternal(clazz, ids, includes, transformer, null);
  }

  @SuppressWarnings({"null", "unchecked", "rawtypes"})
  private <T> T[] loadInternal(final Class<T> clazz, final String[] ids, List<Tuple<String, Class>> includes, final String transformer, final Map<String, RavenJToken> transformerParameters) {

    T[] results = (T[]) Array.newInstance(clazz, ids.length);
    final String[] includePaths = includes != null ? new String[includes.size()] : null;
    if (includes != null) {
      for (int i = 0; i < includes.size(); i++) {
        includePaths[i] = includes.get(i).getItem1();
      }
    }

    Map<List<IDatabaseCommands>, List<IdToLoad<IDatabaseCommands>>> idsToLoad = getIdsThatNeedLoading(clazz, ids, includePaths, transformer);
    if (idsToLoad.isEmpty()) {
      return results;
    }

    incrementRequestCount();

    if (clazz.isArray()) {
      for (Entry<List<IDatabaseCommands>, List<IdToLoad<IDatabaseCommands>>> shard: idsToLoad.entrySet()) {
        final List<String> currentShardIds = new ArrayList<>();
        for (IdToLoad<IDatabaseCommands> x: shard.getValue()) {
          currentShardIds.add(x.id);
        }
        ShardRequestData shardRequest = new ShardRequestData();
        shardRequest.setEntityType(clazz);
        shardRequest.setKeys(currentShardIds);
        T[][] shardResults = getShardStrategy().getShardAccessStrategy().apply(clazz, shard.getKey(), shardRequest, new Function2<IDatabaseCommands, Integer, T[]>() {
          @SuppressWarnings("synthetic-access")
          @Override
          public T[] apply(IDatabaseCommands dbCmd, Integer i) {
            // Returns array of arrays, public APIs don't surface that yet though as we only support Transform
            // With a single Id
            List<RavenJObject> dbGetResults = dbCmd.get(currentShardIds.toArray(new String[0]), includePaths, transformer, transformerParameters)
              .getResults();

            List<T> items = new ArrayList<>();

            for (RavenJObject result : dbGetResults) {
              List<RavenJObject> values = result.value(RavenJArray.class, "$values").values(RavenJObject.class);
              List<Object> innerTypes = new ArrayList<>();
              for (RavenJObject value: values) {
                handleInternalMetadata(value);
                innerTypes.add(convertToEntity(clazz, null, value, new RavenJObject()));
              }
              Object[] innerArray = (Object[]) Array.newInstance(clazz, innerTypes.size());
              for (int j = 0; j < innerTypes.size(); j++) {
                innerArray[j] = innerTypes.get(j);
              }
              items.add((T) innerArray);
            }

            return (T[]) items.toArray();
          }
        });
        int items = 0;
        for (T[] item: shardResults) {
          items += item.length;
        }
        T[] finalResult = (T[]) Array.newInstance(clazz, items);
        int i = 0;
        for (T[] itemOuter: shardResults) {
          for (T itemInner: itemOuter) {
            finalResult[i] = itemInner;
            i++;
          }
        }
        return finalResult;
      }
    }

    for (Entry<List<IDatabaseCommands>, List<IdToLoad<IDatabaseCommands>>> shard: idsToLoad.entrySet()) {
      final List<String> currentShardIds = new ArrayList<>();
      for (IdToLoad<IDatabaseCommands> x: shard.getValue()) {
        currentShardIds.add(x.id);
      }
      ShardRequestData shardRequest = new ShardRequestData();
      shardRequest.setEntityType(clazz);
      shardRequest.setKeys(currentShardIds);
      T[][] shardResults = getShardStrategy().getShardAccessStrategy().apply(clazz, shard.getKey(), shardRequest, new Function2<IDatabaseCommands, Integer, T[]>() {
        @Override
        public T[] apply(IDatabaseCommands dbCmd, Integer i) {
          // Returns array of arrays, public APIs don't surface that yet though as we only support Transform
          // With a single Id
          MultiLoadResult multiLoadResult = dbCmd.get(currentShardIds.toArray(new String[0]), includePaths, transformer, transformerParameters);
          T[] items = new LoadTransformerOperation(ShardedDocumentSession.this, transformer, ids).complete(clazz, multiLoadResult);

          if (items.length > currentShardIds.size()) {
            throw new IllegalStateException(
              "A load was attempted with transformer " + transformer + ", and more than one item was returned per entity - please use " + clazz.getSimpleName()
              + "[] as the projection type instread of " + clazz.getSimpleName());
          }
          return items;
        }
      });

      List<String> idsAsList = Arrays.asList(ids);
      for (T[] shardResult: shardResults) {
        for (int i = 0; i < shardResult.length; i++) {
          if (shardResult[i] == null) {
            continue;
          }
          String id = currentShardIds.get(i);
          int itemPosition = idsAsList.indexOf(id);
          if (results[itemPosition] != null) {
            throw new IllegalStateException("Found document with id: " + id + " on more than a single shard, which is not allowed. Document keys have to be unique cluster-wide");
          }
          results[itemPosition] = shardResult[i];
        }
      }

    }
    return results;
  }

  @SuppressWarnings({"cast", "unchecked"})
  @Override
  public <T> T[] loadInternal(Class<T> clazz, String[] ids) {
    return loadInternal(clazz, ids, (Tuple<String, Class<?>>[]) new Tuple[0]);
  }

  @SuppressWarnings({"unchecked", "null"})
  @Override
  public <T> T[] loadInternal(Class<T> clazz, String[] ids, final Tuple<String, Class<?>>[] includes) {
    List<String> idsAsArray = Arrays.asList(ids);
    T[] results = (T[]) Array.newInstance(clazz, ids.length);
    final String[] includePaths = includes != null ? new String[includes.length] : null;
    if (includes != null) {
      for (int i = 0; i < includes.length; i++) {
        includePaths[i] = includes[i].getItem1();
      }
    }

    Map<List<IDatabaseCommands>, List<IdToLoad<IDatabaseCommands>>> idsToLoad = getIdsThatNeedLoading(clazz, ids, includePaths, null);
    if (idsToLoad.isEmpty()) {
      return results;
    }

    incrementRequestCount();

    for (Entry<List<IDatabaseCommands>, List<IdToLoad<IDatabaseCommands>>> shard: idsToLoad.entrySet()) {
      final List<String> currentShardIds = new ArrayList<>();
      for (IdToLoad<IDatabaseCommands> x: shard.getValue()) {
        currentShardIds.add(x.id);
      }
      ShardRequestData shardRequest = new ShardRequestData();
      shardRequest.setEntityType(clazz);
      shardRequest.setKeys(currentShardIds);
      MultiLoadOperation[] multiLoadOperations = getShardStrategy().getShardAccessStrategy().apply(clazz, shard.getKey(), shardRequest, new Function2<IDatabaseCommands, Integer, MultiLoadOperation>() {
        @Override
        public MultiLoadOperation apply(final IDatabaseCommands dbCmd, Integer i) {
          MultiLoadOperation multiLoadOperation = new MultiLoadOperation(ShardedDocumentSession.this, new Function0<CleanCloseable>() {
            @Override
            public CleanCloseable apply() {
              return dbCmd.disableAllCaching();
            }
          }, currentShardIds.toArray(new String[0]), includes);


          MultiLoadResult multiLoadResult;
          do {
            multiLoadOperation.logOperation();
            try (CleanCloseable scope = multiLoadOperation.enterMultiLoadContext()) {
              multiLoadResult = dbCmd.get(currentShardIds.toArray(new String[0]), includePaths);
            }
          } while (multiLoadOperation.setResult(multiLoadResult));
          return multiLoadOperation;
        }
      });

      for (MultiLoadOperation multiLoadOperation : multiLoadOperations) {
        T[] loadResults = multiLoadOperation.complete(clazz);
        for (int i = 0; i < loadResults.length; i++) {
          if (loadResults[i] == null) {
            continue;
          }
          String id = currentShardIds.get(i);
          int itemPosition = idsAsArray.indexOf(id);
          if (results[itemPosition]!= null) {
            throw new IllegalStateException("Found document with id: " + id + " on more than a single shard, which is not allowed. Document keys have to be unique cluster-wide.");
          }
          results[itemPosition] = loadResults[i];
        }
      }
    }


    T[] finalResult = (T[]) Array.newInstance(clazz, ids.length);
    for (int i =0 ; i < finalResult.length; i++) {
      // so we get items that were skipped because they are already in the session cache
      finalResult[i] = (T) entitiesByKey.get(ids[i]);
    }
    return finalResult;
  }

  @Override
  public ILoaderWithInclude include(String path) {
    return new MultiLoaderWithInclude(this).include(path);
  }

  @Override
  public ILoaderWithInclude include(Expression<?> path) {
    return new MultiLoaderWithInclude(this).include(path);
  }

  @Override
  public ILoaderWithInclude include(Class<?> targetClass, Expression<?> path) {
    return new MultiLoaderWithInclude(this).include(targetClass, path);
  }



  public <T> Lazy<T> addLazyOperation(final ILazyOperation operation, final Action1<T> onEval, List<IDatabaseCommands> cmds) {
    pendingLazyOperations.add(Tuple.create(operation, cmds));
    Lazy<T> lazyValue = new Lazy<>(new Function0<T>() {
      @SuppressWarnings("unchecked")
      @Override
      public T apply() {
        executeAllPendingLazyOperations();
        return (T) operation.getResult();
      }
    });
    if (onEval != null) {
      onEvaluateLazy.put(operation, new Action1<Object>() {
        @SuppressWarnings("unchecked")
        @Override
        public void apply(Object first) {
          onEval.apply((T) first);
        }
      });
    }
    return lazyValue;
  }

  protected Lazy<Integer> addLazyCountOperation(final ILazyOperation operation, List<IDatabaseCommands> cmds) {
    pendingLazyOperations.add(Tuple.create(operation, cmds));
    return new Lazy<>(new Function0<Integer>() {
      @SuppressWarnings("boxing")
      @Override
      public Integer apply() {
        executeAllPendingLazyOperations();
        return operation.getQueryResult().getTotalResults();
      }
    });
  }


  @Override
  public <T> Lazy<T[]> lazyLoadInternal(Class<T> clazz, String[] ids, Tuple<String, Class<?>>[] includes,
    Action1<T[]> onEval) {
    final Set<IDatabaseCommands> cmds = new HashSet<>();
    for (String id : ids) {
      ShardRequestData shardRequestData = new ShardRequestData();
      shardRequestData.setEntityType(clazz);
      shardRequestData.setKeys(Arrays.asList(id));
      cmds.addAll(getCommandsToOperateOn(shardRequestData));
    }

    MultiLoadOperation multiLoadOperation = new MultiLoadOperation(this, new Function0<CleanCloseable>() {
      @Override
      public CleanCloseable apply() {
        final List<CleanCloseable> closeables = new ArrayList<>();
        for (IDatabaseCommands cmd: cmds) {
          closeables.add(cmd.disableAllCaching());
        }
        return new CleanCloseable() {
          @Override
          public void close() {
            for (CleanCloseable close: closeables) {
              close.close();
            }
          }
        };
      }
    }, ids, includes);
    LazyMultiLoadOperation<T> lazyOp = new LazyMultiLoadOperation<>(clazz, multiLoadOperation, ids, includes, null);
    return addLazyOperation(lazyOp, onEval, new ArrayList<>(cmds));
  }

  @Override
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(
    Class<TTransformer> tranformerClass, Class<TResult> clazz, String... ids) {
    try {
      return loadInternal(clazz, ids, null, tranformerClass.newInstance().getTransformerName(), null);
    } catch (IllegalAccessException | InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("boxing")
  @Override
  public ResponseTimeInformation executeAllPendingLazyOperations() {
    if (pendingLazyOperations.isEmpty()) {
      return new ResponseTimeInformation();
    }

    try {
      Date sw = new Date();
      incrementRequestCount();
      ResponseTimeInformation responseTimeDuration = new ResponseTimeInformation();
      while (executeLazyOperationsSingleStep()) {
        Thread.sleep(100);
      }
      responseTimeDuration.computeServerTotal();

      for (Tuple<ILazyOperation, List<IDatabaseCommands>> pendingLazyOperation : pendingLazyOperations) {
        Action1<Object> value = onEvaluateLazy.get(pendingLazyOperation.getItem1());
        if (value != null) {
          value.apply(pendingLazyOperation.getItem1().getResult());
        }
      }
      responseTimeDuration.setTotalClientDuration(new Date().getTime() - sw.getTime());
      return responseTimeDuration;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      pendingLazyOperations.clear();
    }
  }

  private boolean executeLazyOperationsSingleStep() {
    List<CleanCloseable> closeables = new ArrayList<>();
    for (Tuple<ILazyOperation, List<IDatabaseCommands>> item :pendingLazyOperations) {
      CleanCloseable context = item.getItem1().enterContext();
      if (context != null) {
        closeables.add(context);
      }
    }

    try {
      Map<List<IDatabaseCommands>, List<ILazyOperation>> operationsPerShardGroup = new TreeMap<>(new DbCmdsListComparer<IDatabaseCommands>());
      for (Tuple<ILazyOperation, List<IDatabaseCommands>> item :pendingLazyOperations) {
        List<ILazyOperation> list = operationsPerShardGroup.get(item.getItem2());
        if (list == null) {
          list = new ArrayList<>();
          operationsPerShardGroup.put(item.getItem2(), list);
        }
        list.add(item.getItem1());
      }

      for (Map.Entry<List<IDatabaseCommands>, List<ILazyOperation>> operationPerShard: operationsPerShardGroup.entrySet()) {
        List<ILazyOperation> lazyOperations = operationPerShard.getValue();
        final List<GetRequest> requests = new ArrayList<>();
        for (ILazyOperation op : lazyOperations) {
          requests.add(op.createRequest());
        }
        GetResponse[][] multiResponses = getShardStrategy().getShardAccessStrategy().apply(GetResponse.class, operationPerShard.getKey(), new ShardRequestData(),
          new Function2<IDatabaseCommands, Integer, GetResponse[]>() {
            @Override
            public GetResponse[] apply(IDatabaseCommands commands, Integer i) {
              return commands.multiGet(requests.toArray(new GetRequest[0]));
            }
          });

        StringBuilder sb = new StringBuilder();
        for (GetResponse[] respGroup: multiResponses) {
          for (GetResponse resp: respGroup)  {
            if (resp.isRequestHasErrors()) {
              sb.append("Get an error form server, status code:" + resp.getStatus() + "\n" + resp.getResult());
              sb.append("\n");
            }
          }
        }

        if (sb.length() > 0) {
          throw new IllegalStateException(sb.toString());
        }

        for (int i = 0; i < lazyOperations.size(); i++) {
          GetResponse[] responses = new GetResponse[multiResponses.length];
          for (int j =0; j < multiResponses.length; j++) {
            responses[j] = multiResponses[j][i];
          }
          lazyOperations.get(i).handleResponses(responses, getShardStrategy());
          if (lazyOperations.get(i).isRequiresRetry()) {
            return true;
          }
        }
      }
      return false;
    } finally {
      for (CleanCloseable closeable: closeables) {
        closeable.close();
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public <T> RavenQueryInspector<T> createRavenQueryInspector() {
    return new ShardedRavenQueryInspector(getShardStrategy(), shardDbCommands.values());
  }

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
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz) {
    String indexName = createDynamicIndexName(clazz);
    return documentQuery(clazz, indexName);
  }

  @Override
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName) {
    return documentQuery(clazz, indexName, false);
  }

  @Override
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, boolean isMapReduce) {
    return new ShardedDocumentQuery<>(clazz, this, new Function1<ShardRequestData, List<Tuple<String, IDatabaseCommands>>>() {
      @Override
      public List<Tuple<String, IDatabaseCommands>> apply(ShardRequestData input) {
        return getShardsToOperateOn(input);
      }
    }, getShardStrategy(), indexName, null, null, theListeners.getQueryListeners(), isMapReduce);
  }

  /**
   * Saves all the changes to the Raven server.
   */
  @Override
  public void saveChanges() {
    try (CleanCloseable scope = entityToJson.entitiesToJsonCachingScope()) {
      SaveChangesData data = prepareForSaveChanges();
      if (data.getCommands().isEmpty() && deferredCommandsByShard.isEmpty()) {
        return; // nothing to do here
      }

      incrementRequestCount();
      logBatch(data);

      // split by shards
      Map<String, SaveChangesData> saveChangesPerShard = getChangesToSavePerShard(data);

      // execute on all shards
      for (Map.Entry<String, SaveChangesData> shardAndObject : saveChangesPerShard.entrySet()) {
        String shardId = shardAndObject.getKey();

        IDatabaseCommands databaseCommands = shardDbCommands.get(shardId);
        if (databaseCommands == null) {
          throw new IllegalStateException("ShardedDocumentStore cannot found a DatabaseCommands for shard id '" + shardId + "'");
        }
        BatchResult[] results = databaseCommands.batch(shardAndObject.getValue().getCommands());
        updateBatchResults(Arrays.asList(results), shardAndObject.getValue());
      }
    }
  }

  @SuppressWarnings("boxing")
  @Override
  public <T> void refresh(final T entity) {
    final DocumentMetadata value = entitiesAndMetadata.get(entity);
    if (value == null) {
      throw new IllegalStateException("Cannot refresh a transient instance");
    }
    incrementRequestCount();

    ShardRequestData shardRequestData = new ShardRequestData(Arrays.asList(value.getKey()), entity.getClass());
    List<IDatabaseCommands> dbCommands = getCommandsToOperateOn(shardRequestData);

    Boolean[] results = getShardStrategy().getShardAccessStrategy().apply(Boolean.class, dbCommands, shardRequestData, new Function2<IDatabaseCommands, Integer, Boolean>() {
      @Override
      public Boolean apply(IDatabaseCommands dbCmd, Integer i) {
        JsonDocument jsonDocument = dbCmd.get(value.getKey());
        if (jsonDocument == null) {
          return Boolean.FALSE;
        }
        value.setMetadata(jsonDocument.getMetadata());
        value.setOriginalMetadata(jsonDocument.getMetadata().cloneToken());
        value.setEtag(jsonDocument.getEtag());
        value.setOriginalValue(jsonDocument.getDataAsJson());
        Object newEntity = convertToEntity(entity.getClass(), value.getKey(), jsonDocument.getDataAsJson(), jsonDocument.getMetadata());
        try {
          for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(entity.getClass()).getPropertyDescriptors()) {
            if (propertyDescriptor.getWriteMethod() == null || propertyDescriptor.getReadMethod() == null) {
              continue;
            }
            Object propValue = propertyDescriptor.getReadMethod().invoke(newEntity, new Object[0]);
            propertyDescriptor.getWriteMethod().invoke(entity, new Object[] { propValue });
          }
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
        return Boolean.TRUE;
      }
    });

    if (results.length > 0) {
      boolean anyTrue = false;
      for (Boolean r : results) {
        if (r)  {
          anyTrue = true;
        }
      }
      if (!anyTrue) {
        throw new IllegalStateException("Document '" + value.getKey() + "' no longer exists and was probably deleted.");
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

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> T[] loadStartingWith(final Class<T> clazz, final String keyPrefix, final String matches, final int start, final int pageSize,
    final String exclude, final RavenPagingInformation pagingInformation, final String skipAfter) {
    incrementRequestCount();
    ShardRequestData shardRequestData = new ShardRequestData(Arrays.asList(keyPrefix), clazz);
    List<IDatabaseCommands> shards = getCommandsToOperateOn(shardRequestData);
    List<JsonDocument>[] results = getShardStrategy().getShardAccessStrategy().apply(List.class, shards, shardRequestData, new Function2<IDatabaseCommands, Integer, List<JsonDocument>>() {
      @Override
      public List<JsonDocument> apply(IDatabaseCommands dbCmd, Integer i) {
        return dbCmd.startsWith(keyPrefix, matches, start, pageSize, false, exclude, pagingInformation, skipAfter, null, null);
      }
    });
    List<T> mergedResult = new ArrayList<>();
    for (List<JsonDocument> docList: results) {
      for (JsonDocument doc: docList) {
        mergedResult.add((T) trackEntity(clazz, doc));
      }
    }

    return mergedResult.toArray((T[]) Array.newInstance(clazz, 0));
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
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(
    final Class<TResult> clazz, final Class<TTransformer> transformerClass, final String keyPrefix, final String matches, final int start,
    final int pageSize, final String exclude, final RavenPagingInformation pagingInformation, final LoadConfigurationFactory configure,
    final String skipAfter) {

    try {
      final String transformer = transformerClass.newInstance().getTransformerName();

      final RavenLoadConfiguration configuration = new RavenLoadConfiguration();
      if (configure != null) {
        configure.configure(configuration);
      }

      incrementRequestCount();
      List<IDatabaseCommands> shards = getCommandsToOperateOn(new ShardRequestData(Arrays.asList(keyPrefix), clazz));

      List<JsonDocument>[] results = getShardStrategy().getShardAccessStrategy().apply(clazz, shards, new ShardRequestData(Arrays.asList(keyPrefix), clazz),
        new Function2<IDatabaseCommands, Integer, List<JsonDocument>>() {
        @Override
        public List<JsonDocument> apply(IDatabaseCommands dbCmd, Integer i) {
          return dbCmd.startsWith(keyPrefix, matches, start, pageSize, false, exclude, pagingInformation, transformer, configuration.getTransformerParameters(), skipAfter);
        }
      });

      int targetLength = 0;
      for (List<JsonDocument> l : results) {
        targetLength += l.size();
      }
      TResult[] result = (TResult[]) Array.newInstance(clazz, targetLength);
      int i = 0;
      for (List<JsonDocument> l: results) {
        for (JsonDocument d: l) {
          result[i] = (TResult) trackEntity(clazz, d);
          i++;
        }
      }
      return result;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getDocumentUrl(Object entity) {
    DocumentMetadata value = entitiesAndMetadata.get(entity);
    if (value == null) {
      throw new IllegalArgumentException("The entity is not part of the session");
    }

    String shardId = value.getMetadata().value(String.class, Constants.RAVEN_SHARD_ID);
    IDatabaseCommands commands = shardDbCommands.get(shardId);
    if (commands == null) {
      throw new IllegalStateException("Could not find match shard for shard id: " + shardId);
    }
    return commands.urlFor(value.getKey());
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query,
    Reference<QueryHeaderInformation> queryHeaderInformation) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query,
    Reference<QueryHeaderInformation> queryHeaderInformation) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith,
    String matches) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith,
    String matches, int start) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith,
    String matches, int start, int pageSize) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith,
    String matches, int start, int pageSize, RavenPagingInformation pagingInformation) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith,
    String matches, int start, int pageSize, RavenPagingInformation pagingInformation, String skipAfter) {
    throw new UnsupportedOperationException("Streams are currently not supported by sharded document store");
  }

  @Override
  public FacetResults[] multiFacetedSearch(FacetQuery... queries) {
    throw new UnsupportedOperationException("Multi faceted searching is currenlty not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    String documentId) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    MoreLikeThisQuery parameters) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String documentId) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    Class<? extends AbstractTransformerCreationTask> transformerClass, String documentId) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator,
    Class<? extends AbstractTransformerCreationTask> transformerClass, MoreLikeThisQuery parameters) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, String documentId) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

  @Override
  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, MoreLikeThisQuery parameters) {
    throw new UnsupportedOperationException("MoreLikeThis is currently not supported by sharded document store");
  }

}
