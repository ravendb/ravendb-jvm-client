package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.data.IndexQuery;

/**
 * Information required to resolve the appropriate shard for an entity / entity and key
 */
public class ShardRequestData {

  private List<String> keys = new ArrayList<>();
  @SuppressWarnings("rawtypes")
  private Class entityType;
  private IndexQuery query;
  private String indexName;

  public ShardRequestData(List<String> keys, Class<?> entityType) {
    super();
    this.keys = keys;
    this.entityType = entityType;
  }

  public ShardRequestData() {
    super();
  }

  public List<String> getKeys() {
    return keys;
  }

  public void setKeys(List<String> keys) {
    this.keys = keys;
  }

  @SuppressWarnings("rawtypes")
  public Class getEntityType() {
    return entityType;
  }

  @SuppressWarnings("rawtypes")
  public void setEntityType(Class entityType) {
    this.entityType = entityType;
  }

  public IndexQuery getQuery() {
    return query;
  }

  public void setQuery(IndexQuery query) {
    this.query = query;
  }

  public String getIndexName() {
    return indexName;
  }

  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

}
