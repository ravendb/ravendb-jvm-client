package net.ravendb.client.shard;

import java.util.HashMap;
import java.util.Map;

import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.MultiTypeHiLoKeyGenerator;


public class ShardedHiloKeyGenerator {
  private final ShardedDocumentStore shardedDocumentStore;
  private final int capacity;
  private Map<String, MultiTypeHiLoKeyGenerator> generatorsByShard = new HashMap<>();

  public ShardedHiloKeyGenerator(ShardedDocumentStore shardedDocumentStore, int capacity) {
    this.shardedDocumentStore = shardedDocumentStore;
    this.capacity = capacity;
  }

  public String generateDocumentKey(IDatabaseCommands databaseCommands, DocumentConvention conventions, Object entity) {
    String shardId = shardedDocumentStore.getShardStrategy().getShardResolutionStrategy().metadataShardIdFor(entity);
    if (shardId == null) {
      throw new IllegalStateException("ShardResolutionStrategy.MetadataShardIdFor cannot return null. You must specify where to store the metadata documents for the entity type " + entity.getClass());
    }

    MultiTypeHiLoKeyGenerator value = generatorsByShard.get(shardId);
    if (value != null) {
      return value.generateDocumentKey(databaseCommands, conventions, entity);
    }

    synchronized (this) {
      value =generatorsByShard.get(shardId);
      if (value == null) {
        value = new MultiTypeHiLoKeyGenerator(capacity);
        HashMap<String, MultiTypeHiLoKeyGenerator> newMap = new HashMap<>(generatorsByShard);
        newMap.put(shardId, value);
        generatorsByShard = newMap;
      }
      return value.generateDocumentKey(databaseCommands, conventions, entity);
    }
  }
}
