package net.ravendb.client.shard;

import java.util.List;

import net.ravendb.abstractions.closure.Function2;
import net.ravendb.client.connection.IDatabaseCommands;

/**
 *  Apply an operation to all the shard session
 */
public interface IShardAccessStrategy {

  public void addOnError(ShardingErrorHandle<IDatabaseCommands> handler);

  public void removeOnError(ShardingErrorHandle<IDatabaseCommands> handler);

  /**
   * Applies the specified action to all shard sessions.
   */
  <T> T[] apply(Class<?> clazz, List<IDatabaseCommands> commands, ShardRequestData request, Function2<IDatabaseCommands, Integer, T> operation);

}
