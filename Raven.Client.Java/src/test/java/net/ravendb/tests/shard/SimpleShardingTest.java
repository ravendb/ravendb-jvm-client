package net.ravendb.tests.shard;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.ravendb.abstractions.commands.DeleteCommandData;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.tests.bugs.User;
import net.ravendb.tests.bundles.replication.ReplicationBase;


public class SimpleShardingTest extends ReplicationBase {

  @Test
  public void canUseDeferred() {
    try (IDocumentStore store1 = createStore();
      IDocumentStore store2 = createStore();
      IDocumentStore store3 = createStore()) {
      Map<String, IDocumentStore> shardMap = new HashMap<>();
      shardMap.put("1", store1);
      shardMap.put("2", store2);
      shardMap.put("3", store3);
      ShardStrategy strategy = new ShardStrategy(shardMap);
      String userId =null;
      try (ShardedDocumentStore documentStore = new ShardedDocumentStore(strategy)) {
        documentStore.initialize();
        try (IDocumentSession session = documentStore.openSession()) {
          User user = new User();
          session.store(user);
          userId = user.getId();
          session.saveChanges();
        }

        try (IDocumentSession session = documentStore.openSession()) {
          assertNotNull(session.load(User.class, userId));
        }

        try (IDocumentSession session = documentStore.openSession()) {
          DeleteCommandData deleteCommandData = new DeleteCommandData();
          deleteCommandData.setKey(userId);
          session.advanced().defer(deleteCommandData);
          session.saveChanges();
        }

        try (IDocumentSession session = documentStore.openSession()) {
          assertNull(session.load(User.class, userId));
        }
      }
    }
  }
}
