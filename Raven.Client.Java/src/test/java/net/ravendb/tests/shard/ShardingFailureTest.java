package net.ravendb.tests.shard;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.querying.ContainsAllAndAnyTest.User;
import net.ravendb.client.shard.ParallelShardAccessStrategy;
import net.ravendb.client.shard.ShardRequestData;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.client.shard.ShardingErrorHandle;
import net.ravendb.tests.bundles.replication.ReplicationBase;

import org.junit.Test;


public class ShardingFailureTest extends ReplicationBase {

  @Test
  public void canIgnore() {
    try (IDocumentStore store1 = createStore()) {
      Map<String, IDocumentStore> shardMap = new HashMap<>();
      shardMap.put("one", store1);
      shardMap.put("two", new DocumentStore("http://localhost:9072"));
      ShardStrategy shardStrategy = new ShardStrategy(shardMap);
      shardStrategy.getShardAccessStrategy().addOnError(new ShardingErrorHandle<IDatabaseCommands>() {
        @SuppressWarnings("boxing")
        @Override
        public Boolean apply(IDatabaseCommands first, ShardRequestData second, Exception third) {
          return second != null;
        }
      });

      try (IDocumentStore docStore = new ShardedDocumentStore(shardStrategy).initialize()) {
        try (IDocumentSession session = docStore.openSession()) {
          session.query(User.class).toList();
        }
      }
    }
  }

  @Test
  public void canIgnoreParallel() {
    try (IDocumentStore store1 = createStore()) {
      Map<String, IDocumentStore> shardMap = new HashMap<>();
      shardMap.put("one", store1);
      shardMap.put("two", new DocumentStore("http://localhost:9072"));
      ShardStrategy shardStrategy = new ShardStrategy(shardMap);
      try (ParallelShardAccessStrategy accessStrategy = new ParallelShardAccessStrategy()) {
        shardStrategy.setShardAccessStrategy(accessStrategy);
        shardStrategy.getShardAccessStrategy().addOnError(new ShardingErrorHandle<IDatabaseCommands>() {
          @SuppressWarnings("boxing")
          @Override
          public Boolean apply(IDatabaseCommands first, ShardRequestData second, Exception third) {
            return second != null;
          }
        });

        try (IDocumentStore docStore = new ShardedDocumentStore(shardStrategy).initialize()) {
          try (IDocumentSession session = docStore.openSession()) {
            session.query(User.class).toList();
          }
        }
      }
    }
  }

  @Test
  public void canIgnoreLazy() {
    try (IDocumentStore store1 = createStore()) {
      Map<String, IDocumentStore> shardMap = new HashMap<>();
      shardMap.put("one", store1);
      shardMap.put("two", new DocumentStore("http://localhost:9072"));
      ShardStrategy shardStrategy = new ShardStrategy(shardMap);
      shardStrategy.getShardAccessStrategy().addOnError(new ShardingErrorHandle<IDatabaseCommands>() {
        @SuppressWarnings("boxing")
        @Override
        public Boolean apply(IDatabaseCommands first, ShardRequestData second, Exception third) {
          return second != null;
        }
      });

      try (IDocumentStore docStore = new ShardedDocumentStore(shardStrategy).initialize()) {
        try (IDocumentSession session = docStore.openSession()) {
          Lazy<List<User>> lazily = session.query(User.class).lazily();
          assertNotNull(lazily.getValue());
        }
      }
    }
  }
}
