package net.ravendb.client.shard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.extensions.ExpressionExtensions;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;

import com.mysema.query.types.Path;

public class DefaultShardResolutionStrategy implements IShardResolutionStrategy {

  private final ShardStrategy shardStrategy;
  protected final List<String> shardIds;

  private final Map<Class<?>, Pattern> regexToCaptureShardIdFromQueriesByType = new HashMap<>();
  private final Map<Class<?>, Function1<Object, String>> shardResultToStringByType = new HashMap<>();
  private final Map<Class<?>, Function1<String, String>> queryResultToStringByType = new HashMap<>();

  public DefaultShardResolutionStrategy(Collection<String> shardIds, ShardStrategy shardStrategy) {
    this.shardStrategy = shardStrategy;
    this.shardIds = new ArrayList<>(shardIds);
    if (shardIds.isEmpty()) {
      throw new IllegalArgumentException("shardIds must have at least one value");
    }
  }

  public <TResult> DefaultShardResolutionStrategy shardingOn(Path<TResult> shardingProperty) {
    return shardingOn(shardingProperty, null, null);
  }

  @SuppressWarnings("boxing")
  public <TResult> DefaultShardResolutionStrategy shardingOn(final Path<TResult> shardingProperty, Function1<TResult, String> valueTranslator,
    Function1<String, String> queryTranslator) {

    if (valueTranslator == null) {
      valueTranslator = new Function1<TResult, String>() {
        @SuppressWarnings("synthetic-access")
        @Override
        public String apply(TResult result) {
          if (result == null) {
            throw new IllegalStateException("Got null for the shard id in the value translator using "
              + ExpressionExtensions.toPropertyPath(shardingProperty)
              + ", no idea how to get the shard id from null.");
          }
          // by default we assume that if you have a separator in the value we got back
          // the shard id is the very first value up until the first separator
          String str = result.toString();
          int start = str.toLowerCase().indexOf(shardStrategy.getConventions().getIdentityPartsSeparator().toLowerCase());
          if (start == -1) {
            return str;
          }
          return str.substring(0, start);
        }
      };
    }

    final Function1<TResult, String> finalValueTranslator = valueTranslator;

    if (queryTranslator == null) {
      queryTranslator = new Function1<String, String>() {
        @SuppressWarnings("unchecked")
        @Override
        public String apply(String result) {
          return finalValueTranslator.apply((TResult) ConvertUtils.convert(result, shardingProperty.getType()));
        }
      };
    }

    String shardFieldForQuerying = ExpressionExtensions.toPropertyPath(shardingProperty);

    if (shardStrategy.getConventions().getFindIdentityProperty().find(ExpressionExtensions.toProperty(shardingProperty))) {
      shardFieldForQuerying = Constants.DOCUMENT_ID_FIELD_NAME;
    }

    String pattern =Pattern.quote(shardFieldForQuerying) + ":\\s*\"?([^\"]*)\"?";
    Pattern compiledPattern = Pattern.compile(pattern);
    regexToCaptureShardIdFromQueriesByType.put(shardingProperty.getRoot().getType(), compiledPattern);

    shardResultToStringByType.put(shardingProperty.getRoot().getType(), new Function1<Object, String>() {
      @SuppressWarnings("unchecked")
      @Override
      public String apply(Object input) {
        try {
          Field property = ExpressionExtensions.toProperty(shardingProperty);
          property.setAccessible(true);
          Object value = property.get(input);
          return finalValueTranslator.apply((TResult)value);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(e);
        }
      }
    });
    queryResultToStringByType.put(shardingProperty.getRoot().getType(), queryTranslator);
    return this;

  }

  /**
   * Generate a shard id for the specified entity
   */
  @Override
  public String generateShardIdFor(Object entity, Object owner) {
    if (shardResultToStringByType.isEmpty()) {
      // one shard per session
      return shardIds.get(owner.hashCode() % shardIds.size());
    }

    Function1<Object, String> func = shardResultToStringByType.get(entity.getClass());
    if (func == null) {
      throw new IllegalStateException("Entity " + entity.getClass().getName() + " was not setup in " + getClass().getName() + " even though other entities have been setup using shardingOn."
        + "Did you forget to call shardingOn(" + entity.getClass().getName() + ") and provide the sharding fuction required?");
    }
    return func.apply(entity);
  }

  /**
   * The shard id for the server that contains the metadata (such as the HiLo documents)
   *  for the given entity
   */
  @Override
  public String metadataShardIdFor(Object entity) {
    return shardIds.isEmpty() ? null : shardIds.get(0);
  }

  /**
   *  Selects the shard ids appropriate for the specified data.
   *  @return Return a list of shards ids that will be search. Returning null means search all shards.
   */
  @Override
  public List<String> potentialShardsFor(ShardRequestData requestData) {
    if (requestData.getQuery() != null) {
      Pattern regex = regexToCaptureShardIdFromQueriesByType.get(requestData.getEntityType());
      if (regex == null) {
        return null; // we have no special knowledge, let us just query everything
      }

      Matcher collection = regex.matcher(requestData.getQuery().getQuery());

      Function1<String, String> translateQueryValueToShardId = queryResultToStringByType.get(requestData.getEntityType());

      List<String> potentialShardsFor = new ArrayList<>();
      while (collection.find()) {
        String shardId = collection.group(1);
        if (StringUtils.isNotEmpty(shardId)) {
          potentialShardsFor.add(translateQueryValueToShardId.apply(shardId));
        }
      }

      if (potentialShardsFor.size() == 0) {
        return null;
      }

      for (String queryShardId : potentialShardsFor) {
        if (!shardIds.contains(queryShardId)) {
          return null; // we couldn't find the shard ids here, maybe there is something wrong in the query, sending to all shards
        }
      }
      return potentialShardsFor;
    }

    if (requestData.getKeys().isEmpty()) { // we are only optimized for keys
      return null;
    }

    // we are looking for search by key, let us see if we can narrow it down by using the
    // embedded shard id.
    List<String> list = new ArrayList<>();
    for (String key: requestData.getKeys()) {
      int start = key.toLowerCase().indexOf(shardStrategy.getConventions().getIdentityPartsSeparator().toLowerCase());
      if (start == -1) {
        return null; //if we couldn't figure it out, select from all
      }
      String maybeShardId = key.substring(0, start);
      if (shardIds.contains(maybeShardId)) {
        list.add(maybeShardId);
      } else {
        return null; // we couldn't find it there, select from all
      }
    }
    return list;
  }
}
