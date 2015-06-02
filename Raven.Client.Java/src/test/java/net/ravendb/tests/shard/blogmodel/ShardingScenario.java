package net.ravendb.tests.shard.blogmodel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.document.FailoverBehaviorSet;
import net.ravendb.client.shard.SequentialShardAccessStrategy;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.tests.bundles.replication.ReplicationBase;


public abstract class ShardingScenario extends ReplicationBase {

  protected ShardedDocumentStore shardedDocumentStore;
  protected Map<String, IDocumentStore> servers;

  @SuppressWarnings("unused")
  public void createDefaultIndexes(IDocumentStore documentStore) {
    // empty by design
  }

  @Override
  @Before
  public void init() {
    super.init();
    IDocumentStore users = null;
    IDocumentStore blogs = null;
    IDocumentStore posts1 = null;
    IDocumentStore posts2 = null;
    IDocumentStore posts3 = null;
    try {
      Action1<DocumentStore> setupRepliaction = new Action1<DocumentStore>() {
        @Override
        public void apply(DocumentStore store) {
          store.getConventions().setFailoverBehavior(FailoverBehaviorSet.of(FailoverBehavior.FAIL_IMMEDIATELY));
        }
      };

      users = createStore(setupRepliaction);
      blogs = createStore(setupRepliaction);
      posts1 = createStore(setupRepliaction);
      posts2 = createStore(setupRepliaction);
      posts3 = createStore(setupRepliaction);
    } catch (Exception e) {
      if (users != null) {
        users.close();
      }
      if (blogs != null)  {
        blogs.close();
      }
      if (posts1 != null) {
        posts1.close();
      }
      if (posts2 != null) {
        posts2.close();
      }
      if (posts3 != null) {
        posts3.close();
      }
    }

    servers = new LinkedHashMap<>();
    servers.put("Users", users);
    servers.put("Blogs", blogs);
    servers.put("Posts01", posts1);
    servers.put("Posts02", posts2);
    servers.put("Posts03", posts3);

    ShardStrategy strategy = new ShardStrategy(servers);
    strategy.setShardAccessStrategy(new SequentialShardAccessStrategy());
    strategy.setShardResolutionStrategy(new BlogShardResolutionStrategy(3));
    shardedDocumentStore = new ShardedDocumentStore(strategy);
    shardedDocumentStore.initialize();
  }

  @Override
  @After
  public void afterTest() {
    shardedDocumentStore.close();
    super.afterTest();
  }

}
