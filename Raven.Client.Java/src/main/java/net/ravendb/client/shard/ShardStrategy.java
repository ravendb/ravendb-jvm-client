package net.ravendb.client.shard;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.data.SortedField;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.utils.encryptors.Encryptor;

import org.apache.commons.lang.NullArgumentException;

import com.mysema.query.types.Path;
import com.mysema.query.types.path.PathBuilder;

public class ShardStrategy {
  private final Map<String, IDocumentStore> shards;
  private final List<String> shardsKeys;
  private DocumentConvention conventions;
  private MergeQueryResultsFunc mergeQueryResults;
  private IShardResolutionStrategy shardResolutionStrategy;
  private IShardAccessStrategy shardAccessStrategy;
  private ModifyDocumentIdFunc modifyDocumentId;

  public ShardStrategy(Map<String, IDocumentStore> shards) {
    if (shards == null) {
      throw new NullArgumentException("shards");
    }
    if (shards.isEmpty()) {
      throw new IllegalArgumentException("Shards collection must have at least one item");
    }
    this.shards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    this.shards.putAll(shards);
    this.shardsKeys = new ArrayList<>(shards.keySet());

    conventions = shards.entrySet().iterator().next().getValue().getConventions().clone();

    shardAccessStrategy = new SequentialShardAccessStrategy();
    shardResolutionStrategy = new DefaultShardResolutionStrategy(shards.keySet(), this);
    mergeQueryResults = new MergeQueryResultsFunc() {
      @Override
      public QueryResult apply(IndexQuery query, List<QueryResult> queryResults) {
        return defaultMergeQueryResults(query, queryResults);
      }
    };
    modifyDocumentId = new ModifyDocumentIdFunc() {
      @Override
      public String apply(DocumentConvention convention, String shardId, String documentId) {
        return documentId.startsWith(shardId + convention.getIdentityPartsSeparator())?documentId : shardId + convention.getIdentityPartsSeparator() + documentId;
      }
    };
  }

  public DocumentConvention getConventions() {
    return conventions;
  }

  public void setConventions(DocumentConvention conventions) {
    this.conventions = conventions;
  }

  /**
   * Merge the query results from all the shards into a single query results object
   */
  public MergeQueryResultsFunc getMergeQueryResults() {
    return mergeQueryResults;
  }

  /**
   * Merge the query results from all the shards into a single query results object
   */
  public void setMergeQueryResults(MergeQueryResultsFunc mergeQueryResults) {
    this.mergeQueryResults = mergeQueryResults;
  }

  /**
   *  Merge the query results from all the shards into a single query results object by simply
   *  concatenating all of the values
   */
  @SuppressWarnings("boxing")
  public QueryResult defaultMergeQueryResults(IndexQuery query, List<QueryResult> queryResults) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      for (QueryResult result : queryResults) {
        baos.write(result.getIndexEtag().toByteArray());
      }
      byte[] hash = Encryptor.getCurrent().createHash().compute16(baos.toByteArray());
      Etag indexEtag = Etag.parse(hash);

      List<RavenJObject> results = new ArrayList<>();
      for (QueryResult interResult: queryResults) {
        results.addAll(interResult.getResults());
      }

      // apply sorting
      if (query.getSortedFields() != null) {
        final List<String> fieldsToSort = new ArrayList<>();
        final List<Integer> desceding = new ArrayList<>();
        for (SortedField field: query.getSortedFields()) {
          String f = field.getField();
          if (f.endsWith("_Range")) {
            f = f.substring(0, f.length() - "_Range".length());
          }
          fieldsToSort.add(f);
          desceding.add(field.isDescending() ? -1 : 1);
        }

        Collections.sort(results, new Comparator<RavenJObject>() {
          @Override
          public int compare(RavenJObject o1, RavenJObject o2) {
            for (int i = 0; i < fieldsToSort.size(); i++) {
              int orderSignum = desceding.get(i);
              RavenJToken t1 = o1.selectTokenWithRavenSyntaxReturningSingleValue(fieldsToSort.get(i));
              RavenJToken t2 = o2.selectTokenWithRavenSyntaxReturningSingleValue(fieldsToSort.get(i));
              if (t1.getType() == JTokenType.INTEGER && t2.getType() == JTokenType.INTEGER) {
                int result = t1.value(Integer.class).compareTo(t2.value(Integer.class)) * orderSignum;
                if (result != 0) {
                  return result;
                }
              } else {
                int result = t1.toString().compareTo(t2.toString()) * orderSignum;
                if (result != 0) {
                  return result;
                }
              }
            }
            return 0;
          }
        });
      }

      QueryResult mergedQueryResult = new QueryResult();
      List<RavenJObject> includes = new ArrayList<>();
      for (QueryResult queryResult: queryResults) {
        includes.addAll(queryResult.getIncludes());
      }
      mergedQueryResult.setIncludes(includes);
      mergedQueryResult.setResults(results);

      boolean isStale = false;
      int totalResults = 0;
      int skippedResults = Integer.MAX_VALUE;
      Date indexTimestamp = null;
      String indexName = null;

      for (QueryResult r: queryResults) {
        indexName = r.getIndexName();
        if (indexTimestamp == null || r.getIndexTimestamp().before(indexTimestamp)) {
          indexTimestamp = r.getIndexTimestamp();
        }
        isStale |= r.isStale();
        totalResults += r.getTotalResults();
        if (skippedResults > r.getSkippedResults()) {
          skippedResults = r.getSkippedResults();
        }
      }

      mergedQueryResult.setIndexName(indexName);
      mergedQueryResult.setIndexTimestamp(indexTimestamp);
      mergedQueryResult.setStale(isStale);
      mergedQueryResult.setTotalResults(totalResults);
      mergedQueryResult.setIndexEtag(indexEtag);
      mergedQueryResult.setSkippedResults(skippedResults);

      return mergedQueryResult;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public IShardResolutionStrategy getShardResolutionStrategy() {
    return shardResolutionStrategy;
  }


  public void setShardResolutionStrategy(IShardResolutionStrategy shardResolutionStrategy) {
    this.shardResolutionStrategy = shardResolutionStrategy;
  }

  public ModifyDocumentIdFunc getModifyDocumentId() {
    return modifyDocumentId;
  }

  public void setModifyDocumentId(ModifyDocumentIdFunc modifyDocumentId) {
    this.modifyDocumentId = modifyDocumentId;
  }

  public IShardAccessStrategy getShardAccessStrategy() {
    return shardAccessStrategy;
  }

  public void setShardAccessStrategy(IShardAccessStrategy shardAccessStrategy) {
    this.shardAccessStrategy = shardAccessStrategy;
  }

  public Map<String, IDocumentStore> getShards() {
    return shards;
  }

  /**
   * Instructs the sharding strategy to shard the entityClazz instances based on
   * round robin strategy.
   */
  public <T> ShardStrategy shardingOn(final Class<T> entityClazz) {
    if (!(shardResolutionStrategy instanceof DefaultShardResolutionStrategy)) {
      throw new UnsupportedOperationException("ShadringOn is only supported if ShardResolutionStrategy is DefaultShardResultionStrategy");
    }
    DefaultShardResolutionStrategy defaultShardResolutionStrategy = (DefaultShardResolutionStrategy) shardResolutionStrategy;
    Field identityProperty = getConventions().getIdentityProperty(entityClazz);
    if (identityProperty == null) {
      throw new IllegalArgumentException("Cannot set default sharding on " + entityClazz.getSimpleName()
        + " because RavenDB was unable to figure out what the identity property of this entity is.");
    }

    PathBuilder<T> pathBuilder = new PathBuilder<>(entityClazz, "p");
    final PathBuilder<Object> propExpression = pathBuilder.get(identityProperty.getName());

    defaultShardResolutionStrategy.shardingOn(propExpression, new Function1<Object, String>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public String apply(Object result) {
        if (result == null) {
          throw new IllegalStateException("Got null for the shard id in the value translator for " +
              entityClazz + " using " + propExpression +", no idea how to get the shard if from null.");
        }
        int shardNum = Math.abs(stableHashString(result.toString()) % getShards().size());
        return shardsKeys.get(shardNum);
      }
    }, null);
    return this;
  }

  /**
   * Instructs the sharding strategy to shard the T instances based on
   * the property specified in shardingProperty
   */
  public <T, S> ShardStrategy shardingOn(Path<S> shardingProperty) {
    return shardingOn(shardingProperty, null, null);
  }

  /**
   * Instructs the sharding strategy to shard the T instances based on
   * the property specified in shardingProperty
   */
  public <T, S> ShardStrategy shardingOn(Path<S> shardingProperty, Function1<S, String> valueTranslator) {
    return shardingOn(shardingProperty, valueTranslator, null);
  }

  /**
   * Instructs the sharding strategy to shard the T instances based on
   * the property specified in shardingProperty
   */
  public <T, S> ShardStrategy shardingOn(Path<S> shardingProperty, Function1<S, String> valueTranslator,
    Function1<String, String> queryTranslator) {
    if (!(shardResolutionStrategy instanceof DefaultShardResolutionStrategy)) {
      throw new UnsupportedOperationException("ShadringOn is only supported if ShardResolutionStrategy is DefaultShardResultionStrategy");
    }
    DefaultShardResolutionStrategy defaultShardResolutionStrategy = (DefaultShardResolutionStrategy) shardResolutionStrategy;
    defaultShardResolutionStrategy.shardingOn(shardingProperty, valueTranslator, queryTranslator);
    return this;
  }

  public int stableHashString(String text) {
    int hash = 11;
    for (char c: text.toCharArray()) {
      hash = hash * 397 + c;
    }
    return hash;
  }
}
