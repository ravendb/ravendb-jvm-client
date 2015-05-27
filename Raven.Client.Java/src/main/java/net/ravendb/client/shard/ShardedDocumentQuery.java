package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentQuery;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;
import net.ravendb.client.document.batches.LazyQueryOperation;
import net.ravendb.client.document.sessionoperations.QueryOperation;
import net.ravendb.client.listeners.IDocumentQueryListener;

import com.google.common.collect.Iterables;


public class ShardedDocumentQuery<T> extends DocumentQuery<T> {
  private final Function1<ShardRequestData, List<Tuple<String, IDatabaseCommands>>> getShardsToOperateOn;
  private final ShardStrategy shardStrategy;
  private List<QueryOperation> shardQueryOperations;

  private List<IDatabaseCommands> databaseCommands;

  private List<IDatabaseCommands> getShardDatabaseCommands() {
    if (databaseCommands == null) {
      ShardRequestData shardRequestData = new ShardRequestData();
      shardRequestData.setEntityType(getElementType());
      shardRequestData.setQuery(getIndexQueryProp());
      shardRequestData.setIndexName(indexName);
      List<Tuple<String, IDatabaseCommands>> shardsToOperateOn = getShardsToOperateOn.apply(shardRequestData);
      List<IDatabaseCommands> commands = new ArrayList<>(shardsToOperateOn.size());
      for (Tuple<String, IDatabaseCommands> shard: shardsToOperateOn) {
        commands.add(shard.getItem2());
      }
      databaseCommands = commands;
    }
    return databaseCommands;
  }

  private IndexQuery indexQuery;

  private IndexQuery getIndexQueryProp() {
    if (indexQuery != null) {
      return indexQuery;
    }
    indexQuery = generateIndexQuery(queryText.toString());
    return indexQuery;
  }

  public ShardedDocumentQuery(Class<T> clazz, InMemoryDocumentSessionOperations session, Function1<ShardRequestData,
    List<Tuple<String, IDatabaseCommands>>> getShardsToOperateOn, ShardStrategy shardStrategy,
    String indexName, String[] fieldsToFetch, String[] projectionFields,
    List<IDocumentQueryListener> queryListeneres, boolean isMapReduce) {
    super(clazz, session, null, indexName, fieldsToFetch, projectionFields, queryListeneres, isMapReduce);
    this.getShardsToOperateOn = getShardsToOperateOn;
    this.shardStrategy = shardStrategy;
  }

  @Override
  public void initSync() {
    if (queryOperation != null) {
      return;
    }

    shardQueryOperations = new ArrayList<>();
    theSession.incrementRequestCount();

    executeBeforeQueryListeners();

    for (IDatabaseCommands dbCmd: getShardDatabaseCommands()) {
      clearSortHints(dbCmd);
      shardQueryOperations.add(initializeQueryOperation());
    }

    executeActualQuery();
  }

  @Override
  public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String[] fields,
    String[] projections) {
    ShardedDocumentQuery<TProjection> documentQuery = new ShardedDocumentQuery<>(projectionClass,
      theSession, getShardsToOperateOn, shardStrategy, indexName, fields, projections, Arrays.asList(queryListeners), isMapReduce);

    documentQuery.pageSize = pageSize;
    documentQuery.queryText = new StringBuilder(queryText.toString());
    documentQuery.start = start;
    documentQuery.timeout = timeout;
    documentQuery.cutoff = cutoff;
    documentQuery.cutoffEtag = cutoffEtag;
    documentQuery.queryStats = queryStats;
    documentQuery.theWaitForNonStaleResults = theWaitForNonStaleResults;
    documentQuery.theWaitForNonStaleResultsAsOfNow = theWaitForNonStaleResultsAsOfNow;
    documentQuery.sortByHints = sortByHints;
    documentQuery.orderByFields = orderByFields;
    documentQuery.distinct = distinct;
    documentQuery.allowMultipleIndexEntriesForSameDocumentToResultTransformer = allowMultipleIndexEntriesForSameDocumentToResultTransformer;
    documentQuery.transformResultsFunc = transformResultsFunc;
    documentQuery.includes = new HashSet<>(includes);
    documentQuery.rootTypes = new HashSet<>();
    documentQuery.rootTypes.add(getElementType());
    documentQuery.beforeQueryExecutionAction = beforeQueryExecutionAction;
    documentQuery.afterQueryExecutedCallback = afterQueryExecutedCallback;
    documentQuery.afterStreamExecutedCallback = afterStreamExecutedCallback;
    documentQuery.defaultField = defaultField;
    documentQuery.highlightedFields = highlightedFields;
    documentQuery.highlighterPreTags = highlighterPreTags;
    documentQuery.highlighterPostTags = highlighterPostTags;
    documentQuery.distanceErrorPct = distanceErrorPct;
    documentQuery.isSpatialQuery = isSpatialQuery;
    documentQuery.negate = negate;
    documentQuery.queryShape = queryShape;
    documentQuery.spatialFieldName = spatialFieldName;
    documentQuery.spatialRelation = spatialRelation;
    documentQuery.spatialUnits = spatialUnits;
    documentQuery.databaseCommands = databaseCommands;
    documentQuery.indexQuery = indexQuery;
    documentQuery.disableEntitiesTracking = disableEntitiesTracking;
    documentQuery.disableCaching = disableCaching;
    documentQuery.showQueryTimings = showQueryTimings;
    documentQuery.shouldExplainScores = shouldExplainScores;
    documentQuery.resultsTransformer = resultsTransformer;
    documentQuery.transformerParameters = transformerParameters;
    documentQuery.defaultOperator = defaultOperator;
    documentQuery.highlighterKeyName = highlighterKeyName;
    documentQuery.lastEquality = lastEquality;
    return documentQuery;
  }

  @SuppressWarnings("boxing")
  @Override
  protected void executeActualQuery() {
    Boolean[] results = new Boolean[getShardDatabaseCommands().size()];
    for (int i =0 ; i < results.length; i++) {
      results[i] = Boolean.FALSE;
    }
    while (true) {
      final Boolean[] currentCopy = results;
      ShardRequestData shardRequestData = new ShardRequestData();
      shardRequestData.setEntityType(getElementType());
      shardRequestData.setQuery(getIndexQueryProp());
      shardRequestData.setIndexName(indexName);
      results = shardStrategy.getShardAccessStrategy().apply(Boolean.class, getShardDatabaseCommands(), shardRequestData, new Function2<IDatabaseCommands, Integer, Boolean>() {
        @SuppressWarnings({"synthetic-access"})
        @Override
        public Boolean apply(IDatabaseCommands dbCmd, Integer i) {
          if (currentCopy[i]) { // if we already got a good result here, do nothing
            return true;
          }

          QueryOperation queryOp = shardQueryOperations.get(i);
          try (CleanCloseable scope = queryOp.enterQueryContext()) {
            queryOp.logQuery();
            QueryResult result = dbCmd.query(indexName, queryOp.getIndexQuery(), includes.toArray(new String[0]));
            return queryOp.isAcceptable(result);
          }
        }
      });

      boolean allAcceptable = true;
      for (boolean r : results) {
        allAcceptable &= r;
      }
      if (allAcceptable) {
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    assertNoDuplicateIdsInResults(shardQueryOperations);

    List<QueryResult> notEmptyQueryResults = new ArrayList<>();
    for (QueryOperation op: shardQueryOperations) {
      if (op.getCurrentQueryResults() != null) {
        notEmptyQueryResults.add(op.getCurrentQueryResults());
      }
    }
    QueryResult mergedQueryResult = shardStrategy.getMergeQueryResults().apply(getIndexQueryProp(), notEmptyQueryResults);
    shardQueryOperations.get(0).forceResult(mergedQueryResult);
    queryOperation = shardQueryOperations.get(0);
    afterQueryExecutedCallback.apply(mergedQueryResult);
  }

  protected static void assertNoDuplicateIdsInResults(List<QueryOperation> shardQueryOperations) {
    Map<String, Set<QueryOperation>> shardsPerId = new HashMap<>();
    for (QueryOperation shardQueryOperation : shardQueryOperations) {
      QueryResult currentQueryResults = shardQueryOperation.getCurrentQueryResults();
      if (currentQueryResults == null) {
        continue;
      }
      for (RavenJObject include: Iterables.concat(currentQueryResults.getIncludes(), currentQueryResults.getResults())) {
        RavenJObject metadata = include.value(RavenJObject.class, Constants.METADATA);
        if (metadata == null) {
          continue;
        }
        String id = metadata.value(String.class, "@id");
        if (id == null) {
          continue;
        }
        if (!shardsPerId.containsKey(id)) {
          shardsPerId.put(id, new HashSet<QueryOperation>());
        }
        shardsPerId.get(id).add(shardQueryOperation);
      }
    }

    for (Map.Entry<String, Set<QueryOperation>> shardPerId: shardsPerId.entrySet()) {
      if (shardPerId.getValue().size() > 1) {
        throw new IllegalStateException("Found id: " + shardPerId.getKey() + " on more than one shard, documents ids must be unique cluster-wide.");
      }
    }
  }

  @Override
  public IDatabaseCommands getDatabaseCommands() {
    throw new UnsupportedOperationException("Sharded has more than one DatabaseCommands to operate on.");
  }

  /**
   *  Register the query as a lazy query in the session and return a lazy
   *  instance that will evaluate the query only when needed
   */
  @Override
  public Lazy<List<T>> lazily(Action1<List<T>> onEval) {
    LazyQueryOperation<T> lazyQueryOperation = processLazyQuery();
    return ((ShardedDocumentSession) theSession).addLazyOperation(lazyQueryOperation, onEval, getShardDatabaseCommands());
  }

  /**
   * Register the query as a lazy-count query in the session and return a lazy
   * instance that will evaluate the query only when needed
   */
  @Override
  public Lazy<Integer> countLazily() {
    LazyQueryOperation<T> lazyQueryOperation = processLazyQuery();
    return ((ShardedDocumentSession) theSession).addLazyCountOperation(lazyQueryOperation, getShardDatabaseCommands());
  }

  private LazyQueryOperation<T> processLazyQuery() {
    if (queryOperation == null) {
      for (IDatabaseCommands databaseCommands11 : getShardDatabaseCommands()) {
        List<String> keys = new ArrayList<>();
        for (String key: databaseCommands11.getOperationsHeaders().keySet()) {
          if (key.startsWith("SortHint")) {
            keys.add(key);
          }
        }
        for (String key: keys) {
          databaseCommands11.getOperationsHeaders().remove(key);
        }
      }
      executeBeforeQueryListeners();
      queryOperation = initializeQueryOperation();
    }

    LazyQueryOperation<T> lazyQueryOperation = new LazyQueryOperation<>(getElementType(), queryOperation, afterQueryExecutedCallback, includes, getShardDatabaseCommands().get(0).getOperationsHeaders());
    return lazyQueryOperation;
  }


}
