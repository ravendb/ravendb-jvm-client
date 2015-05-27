package net.ravendb.client.shard;

import java.util.List;

import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.QueryResult;


public interface MergeQueryResultsFunc extends Function2<IndexQuery, List<QueryResult>, QueryResult> {
  // empty by design
}
