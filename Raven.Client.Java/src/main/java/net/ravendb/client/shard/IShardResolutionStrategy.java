package net.ravendb.client.shard;

import java.util.List;

/**
 * Implementers of this interface provide a way to decide which shards will be queried
 * for a specified operation
 */
public interface IShardResolutionStrategy {
  /**
   * Generate a shard id for the specified entity
   */
  public String generateShardIdFor(Object entity, Object owner);

  /**
   * The shard id for the server that contains the metadata (such as the HiLo documents)
   * for the given entity
   */
  public String metadataShardIdFor(Object entity);

  /**
   * Selects the shard ids appropriate for the specified data.
   * @return Return a list of shards ids that will be search. Returning null means search all shards.
   */
  public List<String> potentialShardsFor(ShardRequestData requestData);

}
