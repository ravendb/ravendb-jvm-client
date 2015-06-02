package net.ravendb.client.shard;

import java.util.List;

import net.ravendb.abstractions.data.IndexQuery;


public interface ShardReduceFunction {
  List<Object> apply(IndexQuery indexQuery, List<Object> results);
}
