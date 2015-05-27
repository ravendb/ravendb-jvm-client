package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.commands.ICommandData;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.ITransactionalDocumentSession;
import net.ravendb.client.RavenQueryHighlightings;
import net.ravendb.client.RavenQueryStatistics;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;
import net.ravendb.client.document.DocumentSessionListeners;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;
import net.ravendb.client.document.SaveChangesData;
import net.ravendb.client.document.batches.ILazyOperation;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.linq.IDocumentQueryGenerator;
import net.ravendb.client.linq.IRavenQueryable;
import net.ravendb.client.linq.RavenQueryInspector;
import net.ravendb.client.linq.RavenQueryProvider;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;


public abstract class BaseShardedDocumentSession<TDatabaseCommands> extends InMemoryDocumentSessionOperations
  implements IDocumentQueryGenerator, ITransactionalDocumentSession {

  @SuppressWarnings("hiding")
  protected List<Tuple<ILazyOperation, List<TDatabaseCommands>>> pendingLazyOperations = new ArrayList<>();
  @SuppressWarnings("hiding")
  protected final Map<ILazyOperation, Action1<Object>> onEvaluateLazy = new HashMap<>();
  protected final Map<String, List<ICommandData>> deferredCommandsByShard = new HashMap<>();
  private final ShardStrategy shardStrategy;
  protected final Map<String, TDatabaseCommands> shardDbCommands;

  protected BaseShardedDocumentSession(String dbName, ShardedDocumentStore documentStore, DocumentSessionListeners listeners, UUID id,
    ShardStrategy shardStrategy, Map<String, TDatabaseCommands> shardDbCommands) {
    super(dbName, documentStore, listeners, id);
    this.shardStrategy = shardStrategy;
    this.shardDbCommands = shardDbCommands;
  }

  public ShardStrategy getShardStrategy() {
    return shardStrategy;
  }

  @Override
  public String getDatabaseName() {
    return _databaseName;
  }

  protected List<Tuple<String, TDatabaseCommands>> getShardsToOperateOn(ShardRequestData resultionData) {
    List<String> shardIds = shardStrategy.getShardResolutionStrategy().potentialShardsFor(resultionData);
    if (shardIds == null) {
      List<Tuple<String, TDatabaseCommands>> result = new ArrayList<>();
      for (Map.Entry<String, TDatabaseCommands> entry: shardDbCommands.entrySet()) {
        result.add(Tuple.create(entry.getKey(), entry.getValue()));
      }
      return result;
    }
    List<Tuple<String, TDatabaseCommands>> result = new ArrayList<>();

    for (String shardId: shardIds) {
      TDatabaseCommands value = shardDbCommands.get(shardId);
      if (value == null) {
        throw new IllegalStateException("Could not find shard id: " + shardId);
      }
      result.add(Tuple.create(shardId, value));
    }

    return result;
  }

  public List<TDatabaseCommands> getCommandsToOperateOn(ShardRequestData resultionData) {
    List<TDatabaseCommands> result = new ArrayList<>();
    for (Tuple<String, TDatabaseCommands> item: getShardsToOperateOn(resultionData)) {
      result.add(item.getItem2());
    }
    return result;
  }


  protected <T> Map<List<TDatabaseCommands>, List<IdToLoad<TDatabaseCommands>>> getIdsThatNeedLoading(Class<T> clazz, String[] ids, String[] includes, String transformer) {
    String[] idsToLoad;
    if (includes != null || StringUtils.isNotEmpty(transformer)) {
      // need to load everything, for the includes
      idsToLoad = ids;
    } else {
      // only load items which aren't already loaded
      Set<String> toLoad = new HashSet<>();
      for (String id: ids) {
        if (!isLoaded(id)) {
          toLoad.add(id);
        }
      }
      idsToLoad = toLoad.toArray(new String[0]);
    }
    Map<List<TDatabaseCommands>, List<IdToLoad<TDatabaseCommands>>> idsAndShards = new TreeMap<>(new DbCmdsListComparer<TDatabaseCommands>());
    for (String id: idsToLoad) {
      ShardRequestData shardRequestData = new ShardRequestData();
      shardRequestData.setKeys(Arrays.asList(id));
      shardRequestData.setEntityType(clazz);
      List<TDatabaseCommands> shards = getCommandsToOperateOn(shardRequestData);
      IdToLoad<TDatabaseCommands> idToLoadStruct = new IdToLoad<>(id, shards);
      List<IdToLoad<TDatabaseCommands>> list = idsAndShards.get(shards);
      if (list == null) {
        list = new ArrayList<>();
        idsAndShards.put(shards, list);
      }
      list.add(idToLoadStruct);
    }

    return idsAndShards;
  }

  protected String getDynamicIndexName(Class<?> clazz) {
    return createDynamicIndexName(clazz);
  }

  protected Map<String, SaveChangesData> getChangesToSavePerShard(SaveChangesData data) {
    Map<String, SaveChangesData> saveChangesPerShard = new HashMap<>();
    for (Map.Entry<String, List<ICommandData>> deferredCommands : deferredCommandsByShard.entrySet()) {
      SaveChangesData saveChangesData = saveChangesPerShard.get(deferredCommands.getKey());
      if (saveChangesData == null) {
        saveChangesData = new SaveChangesData();
        saveChangesPerShard.put(deferredCommands.getKey(), saveChangesData);
      }
      saveChangesData.setDeferredCommandsCount(saveChangesData.getDeferredCommandsCount() + deferredCommands.getValue().size());
      saveChangesData.getCommands().addAll(deferredCommands.getValue());
    }

    deferredCommandsByShard.clear();

    for (int index = 0; index < data.getEntities().size(); index++) {
      Object entity = data.getEntities().get(index);
      RavenJObject metadata = getMetadataFor(entity);
      String shardId = metadata.value(String.class, Constants.RAVEN_SHARD_ID);

      SaveChangesData shardSaveChangesData = saveChangesPerShard.get(shardId);
      if (shardSaveChangesData == null) {
        shardSaveChangesData = new SaveChangesData();
        saveChangesPerShard.put(shardId, shardSaveChangesData);
      }

      shardSaveChangesData.getEntities().add(entity);
      shardSaveChangesData.getCommands().add(data.getCommands().get(index));

    }
    return saveChangesPerShard;
  }

  @Override
  public void defer(ICommandData... commands) {
    Map<String, List<ICommandData>> cmdsByShard = new HashMap<>();
    for (ICommandData cmd : commands) {
      List<Tuple<String, TDatabaseCommands>> shardsToOperateOn = getShardsToOperateOn(new ShardRequestData(Arrays.asList(cmd.getKey()), null));
      if (shardsToOperateOn.isEmpty()) {
        throw new IllegalStateException("Cannot execute " + cmd.getMethod() + " on " + cmd.getKey() + " because it matched no shards");
      }
      if (shardsToOperateOn.size() > 1) {
        throw new IllegalStateException("Cannot execute " + cmd.getMethod() + " on " + cmd.getKey() + " because it matched multiple shards");
      }
      String shardId = shardsToOperateOn.get(0).getItem1();
      List<ICommandData> cmdsPerShard = cmdsByShard.get(shardId);
      if (cmdsPerShard == null) {
        cmdsPerShard = new ArrayList<>();
        cmdsByShard.put(shardId, cmdsPerShard);
      }
      cmdsPerShard.add(cmd);
    }

    for (Map.Entry<String, List<ICommandData>> cmdByShard : cmdsByShard.entrySet()) {
      List<ICommandData> deferreds = deferredCommandsByShard.get(cmdByShard.getKey());
      if (deferreds == null) {
        deferreds = new ArrayList<>();
        deferredCommandsByShard.put(cmdByShard.getKey(), deferreds);
      }
      deferreds.addAll(cmdByShard.getValue());
    }
  }

  @Override
  protected void storeEntityInUnitOfWork(String id, Object entity, Etag etag, RavenJObject metadata, boolean forceConcurrencyCheck) {
    String modifyDocumentId = null;
    if (id != null) {
      modifyDocumentId = modifyObjectId(id, entity, metadata);
    }
    super.storeEntityInUnitOfWork(modifyDocumentId, entity, etag, metadata, forceConcurrencyCheck);
  }

  protected String modifyObjectId(String id, Object entity, RavenJObject metadata) {
    String shardId = shardStrategy.getShardResolutionStrategy().generateShardIdFor(entity, this);
    if (StringUtils.isEmpty(shardId)) {
      throw new IllegalStateException("Could not find shard id for "  + entity + " because " + shardStrategy.getShardAccessStrategy() + " returned null or empty string for the document shard id");
    }
    metadata.add(Constants.RAVEN_SHARD_ID, shardId);
    String modifyDocumentId = shardStrategy.getModifyDocumentId().apply(getConventions(), shardId, id);
    if (!modifyDocumentId.equals(id)) {
      getGenerateEntityIdOnTheClient().trySetIdentity(entity, modifyDocumentId);
    }

    return modifyDocumentId;
  }

  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName) {
    return query(clazz, indexName, false);
  }

  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName, boolean isMapReduce) {
    RavenQueryStatistics ravenQueryStatistics = new RavenQueryStatistics();
    RavenQueryHighlightings highlightings = new RavenQueryHighlightings();
    RavenQueryProvider<T> ravenQueryProvider = new RavenQueryProvider<>(clazz, this, indexName, ravenQueryStatistics, highlightings, null, isMapReduce);
    RavenQueryInspector<T> inspector = new RavenQueryInspector<>();
    inspector.init(clazz, ravenQueryProvider, ravenQueryStatistics, highlightings, indexName, null, this, null, isMapReduce);
    return inspector;
  }

  public <T> IRavenQueryable<T> query(Class<T> clazz) {
    String indexName = createDynamicIndexName(clazz);
    return query(clazz, indexName).customize(new DocumentQueryCustomizationFactory().transformResults(new Function2<IndexQuery, List<Object>, List<Object>>() {
      @Override
      public List<Object> apply(IndexQuery query, List<Object> results) {
        int size = query.getPageSize();
        if (results.size() > size) {
          List<Object> limitedResults = new ArrayList<>(size);
          for (int i =0; i < size; i++) {
            limitedResults.add(results.get(i));
          }
          return limitedResults;
        } else {
          return results;
        }
      }
    }));
  }

  public <T> IRavenQueryable<T> query(Class<T> clazz, Class<? extends AbstractIndexCreationTask> tIndexCreator) {
    return query(clazz, tIndexCreator, null);
  }

  public <T> IRavenQueryable<T> query(Class<T> clazz, Class<? extends AbstractIndexCreationTask> tIndexCreator, Function2<IndexQuery, List<Object>, List<Object>> reduceFunction) {
    try {
      AbstractIndexCreationTask indexCreator = tIndexCreator.newInstance();
      if (indexCreator.isMapReduce() && reduceFunction == null) {
        throw new NullArgumentException("reduceFunction");
      }
      return query(clazz, indexCreator.getIndexName(), indexCreator.isMapReduce())
        .customize(new DocumentQueryCustomizationFactory().transformResults(reduceFunction));
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(tIndexCreator.getName() + " does not have argumentless constructor.");
    }
  }


  public static class DbCmdsListComparer<TDatabaseCommands> implements Comparator<List<TDatabaseCommands>> {
    @Override
    public int compare(List<TDatabaseCommands> o1, List<TDatabaseCommands> o2) {
      int sizeCompare = Integer.compare(o1.size(), o2.size());
      if (sizeCompare != 0) {
        return sizeCompare;
      }

      for (TDatabaseCommands cmd: o1) {
        if (!o2.contains(cmd)) {
          return -1;
        }
      }
      return 0;
    }
  }

  protected static class IdToLoad<TDatabaseCommands> {
    public final String id;
    public final List<TDatabaseCommands> shards;
    public IdToLoad(String id, List<TDatabaseCommands> shards) {
      this.id = id;
      this.shards = shards;
    }
  }

}
