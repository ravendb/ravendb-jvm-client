package net.ravendb.client.shard;

import net.ravendb.abstractions.closure.Function3;


public interface ShardingErrorHandle<TDatabaseCommands> extends Function3<TDatabaseCommands, ShardRequestData, Exception, Boolean> {
  // empty by design
}
