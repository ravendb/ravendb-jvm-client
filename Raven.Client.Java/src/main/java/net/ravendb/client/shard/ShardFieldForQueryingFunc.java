package net.ravendb.client.shard;

import net.ravendb.abstractions.closure.Function1;


public interface ShardFieldForQueryingFunc extends Function1<Class<?>, String> {
  // empty by design
}
