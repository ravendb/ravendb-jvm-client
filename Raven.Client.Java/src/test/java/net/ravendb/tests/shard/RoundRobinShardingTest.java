package net.ravendb.tests.shard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.document.FailoverBehavior;
import net.ravendb.client.document.FailoverBehaviorSet;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;
import net.ravendb.tests.bundles.replication.ReplicationBase;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;


public class RoundRobinShardingTest extends ReplicationBase {

  protected Map<String, IDocumentStore> stores;

  @QueryEntity
  public static class Post {
    private String id;
    private Date publishedAt;

    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public Date getPublishedAt() {
      return publishedAt;
    }
    public void setPublishedAt(Date publishedAt) {
      this.publishedAt = publishedAt;
    }
  }

  @QueryEntity
  public static class PostComments {
    private String id;
    private String postId;
    private String[] comments;
    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
    public String getPostId() {
      return postId;
    }
    public void setPostId(String postId) {
      this.postId = postId;
    }
    public String[] getComments() {
      return comments;
    }
    public void setComments(String[] comments) {
      this.comments = comments;
    }
  }

  private void executeOnShardedStore(Action1<ShardedDocumentStore> shardedAction) {
    try (IDocumentStore store1 = createStore();
      IDocumentStore store2 = createStore();
      IDocumentStore store3 = createStore()) {
      stores = new LinkedHashMap<>();
      stores.put("one", store1);
      stores.put("two", store2);
      stores.put("tri", store3);

      for (IDocumentStore store: stores.values()) {
        store.getConventions().setFailoverBehavior(FailoverBehaviorSet.of(FailoverBehavior.FAIL_IMMEDIATELY));
      }

      QRoundRobinShardingTest_PostComments postComments = QRoundRobinShardingTest_PostComments.postComments;
      ShardStrategy shardStrategy = new ShardStrategy(stores)
      .shardingOn(Post.class)
      .shardingOn(postComments.postId);

      try (ShardedDocumentStore store = new ShardedDocumentStore(shardStrategy)) {
        store.initialize();

        shardedAction.apply(store);
      }
    }
  }


  @Test
  public void savingTwoPostsWillGoToTwoDifferentServers() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @Override
      public void apply(ShardedDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
          Post p1 = new Post();
          session.store(p1);
          Post p2 = new Post();
          session.store(p2);

          session.saveChanges();

          assertEquals("tri/posts/1", p1.getId());
          assertEquals("two/posts/2", p2.getId());
        }
      }
    });
  }

  @Test
  public void whenQueryingWillGoToTheRightServer() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @Override
      public void apply(ShardedDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
          Post p1 = new Post();
          session.store(p1);
          PostComments pc1 = new PostComments();
          pc1.setPostId(p1.getId());
          session.store(pc1);

          session.saveChanges();
        }

        int[] counts = getRequestsCounts(stores);

        try (IDocumentSession session = store.openSession()) {
          QRoundRobinShardingTest_PostComments p = QRoundRobinShardingTest_PostComments.postComments;
          List<PostComments> posts = session.query(PostComments.class).where(p.postId.eq("tri/posts/1")).toList();
          assertFalse(posts.isEmpty());
        }

        int[] afterCounts = getRequestsCounts(stores);

        assertEquals(1, afterCounts[0] - counts[0]);
        assertEquals(1, afterCounts[1] - counts[1]);
        assertEquals(2, afterCounts[2] - counts[2]);
      }
    });
  }

  @Test
  public void whenQueryingWillGoToTheRightServer_UsingQueryById() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @Override
      public void apply(ShardedDocumentStore store) {
        store.getConventions().setAllowQueriesOnId(true);

        try (IDocumentSession session = store.openSession()) {
          Post p1 = new Post();
          session.store(p1);
          PostComments pc1 = new PostComments();
          pc1.setPostId(p1.getId());
          session.store(pc1);

          session.saveChanges();
        }

        int[] counts = getRequestsCounts(stores);

        try (IDocumentSession session = store.openSession()) {
          QRoundRobinShardingTest_Post p = QRoundRobinShardingTest_Post.post;
          List<Post> posts = session.query(Post.class).where(p.id.eq("tri/posts/1")).toList();
          assertFalse(posts.isEmpty());
        }

        int[] afterCounts = getRequestsCounts(stores);

        assertEquals(1, afterCounts[0] - counts[0]);
        assertEquals(1, afterCounts[1] - counts[1]);
        assertEquals(2, afterCounts[2] - counts[2]);
      }
    });
  }

  @Test
  public void whenQueryingWillGoToTheRightServer_Loading() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @Override
      public void apply(ShardedDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
          Post p1 = new Post();
          session.store(p1);
          PostComments pc1 = new PostComments();
          pc1.setPostId(p1.getId());
          session.store(pc1);

          session.saveChanges();
        }

        int[] counts = getRequestsCounts(stores);

        try (IDocumentSession session = store.openSession()) {
          assertNotNull(session.load(Post.class, "tri/posts/1"));
          assertNotNull(session.load(PostComments.class, "tri/PostComments/1"));
        }

        int[] afterCounts = getRequestsCounts(stores);

        assertEquals(1, afterCounts[0] - counts[0]);
        assertEquals(1, afterCounts[1] - counts[1]);
        assertEquals(3, afterCounts[2] - counts[2]);
      }
    });
  }

  @Test
  public void willGetGoodLocalityOfReference() {
    executeOnShardedStore(new Action1<ShardedDocumentStore>() {
      @Override
      public void apply(ShardedDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
          Post p1 = new Post();
          session.store(p1);

          Post p2 = new Post();
          session.store(p2);

          PostComments pc1 = new PostComments();
          pc1.setPostId(p1.getId());
          session.store(pc1);

          PostComments pc2 = new PostComments();
          pc2.setPostId(p2.getId());
          session.store(pc2);
          session.saveChanges();

          assertEquals("tri/posts/1", p1.getId());
          assertEquals("two/posts/2", p2.getId());

          assertEquals("tri/PostComments/1", pc1.getId());
          assertEquals("two/PostComments/2", pc2.getId());
        }
      }
    });
  }
}
