package net.ravendb.tests.shard.blogmodel;

import static org.junit.Assert.assertEquals;

import java.util.List;

import net.ravendb.client.IDocumentSession;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;

import org.junit.Test;


public class CanQueryOnlyPostsTest extends ShardingScenario {

  @Test
  public void whenStoringPost() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Post post1 = new Post();
      post1.setTitle("Item 1");

      Post post2 = new Post();
      post2.setTitle("Item 2");

      Post post3 = new Post();
      post3.setTitle("Item 3");

      int[] preRequestsCount = getRequestsCounts(servers);

      session.store(post1);
      session.store(post2);
      session.store(post3);

      int[] postRequestsCount = getRequestsCounts(servers);

      assertEquals(3, postRequestsCount[2] - preRequestsCount[2]); //hiLo
      for (int i = 0; i < postRequestsCount.length; i++) {
        if (i != 2) {
          assertEquals(1, postRequestsCount[i] - preRequestsCount[i]);
        }
      }
      preRequestsCount = getRequestsCounts(servers);
      session.saveChanges();
      postRequestsCount = getRequestsCounts(servers);
      assertEquals(1, postRequestsCount[0] - preRequestsCount[0]);
      assertEquals(1, postRequestsCount[1] - preRequestsCount[1]);
      assertEquals(2, postRequestsCount[2] - preRequestsCount[2]);
      assertEquals(2, postRequestsCount[3] - preRequestsCount[3]);
      assertEquals(2, postRequestsCount[4] - preRequestsCount[4]);
    }
  }

  @Test
  public void canSortFromMultipleServers() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Post post1 = new Post();
      post1.setTitle("Item 1");
      post1.setUserId("2");

      Post post2 = new Post();
      post2.setTitle("Item 2");
      post2.setUserId("1");

      Post post3 = new Post();
      post3.setTitle("Item 3");
      post3.setUserId("3");

      session.store(post1);
      session.store(post2);
      session.store(post3);
      session.saveChanges();
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      QPost p = QPost.post;
      List<Post> posts = session.query(Post.class)
        .customize(new DocumentQueryCustomizationFactory().waitForNonStaleResults())
        .orderBy(p.userId.asc())
        .toList();

      assertEquals(3, posts.size());
      assertEquals("Item 2", posts.get(0).getTitle());
      assertEquals("Item 1", posts.get(1).getTitle());
      assertEquals("Item 3", posts.get(2).getTitle());
    }
  }

  @Test
  public void canPageFromMultipleServers() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Post post1 = new Post();
      post1.setTitle("Item 1");
      post1.setUserId("2");

      Post post2 = new Post();
      post2.setTitle("Item 2");
      post2.setUserId("1");

      Post post3 = new Post();
      post3.setTitle("Item 3");
      post3.setUserId("3");

      session.store(post1);
      session.store(post2);
      session.store(post3);
      session.saveChanges();
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      QPost p = QPost.post;
      List<Post> posts = session.query(Post.class)
        .customize(new DocumentQueryCustomizationFactory().waitForNonStaleResults())
        .orderBy(p.userId.asc())
        .take(2)
        .toList();

      assertEquals(2, posts.size());
      assertEquals("Item 2", posts.get(0).getTitle());
      assertEquals("Item 1", posts.get(1).getTitle());
    }
  }

  @Test
  public void whenStoring6PostsInOnetSession_Stores2InEachShard() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      for (int i = 0; i < 6; i++) {
        Post post = new Post();
        post.setId("posts/" + i); //avoid generating an HiLo request.
        post.setTitle("Item " + i);
        session.store(post);
      }

      int[] preRequestCounts = getRequestsCounts(servers);
      session.saveChanges();
      int[] postRequestCounts = getRequestsCounts(servers);

      assertEquals(1, postRequestCounts[0] - preRequestCounts[0]);
      assertEquals(1, postRequestCounts[1] - preRequestCounts[1]);
      assertEquals(2, postRequestCounts[2] - preRequestCounts[2]);
      assertEquals(2, postRequestCounts[3] - preRequestCounts[3]);
      assertEquals(2, postRequestCounts[4] - preRequestCounts[4]);
    }
  }

  @Test
  public void whenStoring6PostsEachInADifferentSession_Stores2InEachShard() {
    int[] preRequestCounts = getRequestsCounts(servers);
    for (int i = 0; i < 6; i++) {
      try (IDocumentSession session = shardedDocumentStore.openSession()) {
        Post post = new Post();
        post.setId("posts/" + i); //avoid generating an HiLo request.
        post.setTitle("Item " + i);
        session.store(post);
        session.saveChanges();
      }
    }

    int[] postRequestCounts = getRequestsCounts(servers);

    assertEquals(1, postRequestCounts[0] - preRequestCounts[0]);
    assertEquals(1, postRequestCounts[1] - preRequestCounts[1]);
    assertEquals(3, postRequestCounts[2] - preRequestCounts[2]);
    assertEquals(3, postRequestCounts[3] - preRequestCounts[3]);
    assertEquals(3, postRequestCounts[4] - preRequestCounts[4]);
  }

  @Test
  public void canMergeResultFromAllPostsShards() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {

      int[] preRequestCounts = getRequestsCounts(servers);
      for (int i = 0; i < 6; i++) {
        Post post = new Post();
        post.setId("posts/" + i); //avoid generating an HiLo request.
        post.setTitle("Item " + i);
        session.store(post);
      }
      session.saveChanges();
      int[] postRequestCounts = getRequestsCounts(servers);

      assertEquals(1, postRequestCounts[0] - preRequestCounts[0]);
      assertEquals(1, postRequestCounts[1] - preRequestCounts[1]);
      assertEquals(2, postRequestCounts[2] - preRequestCounts[2]);
      assertEquals(2, postRequestCounts[3] - preRequestCounts[3]);
      assertEquals(2, postRequestCounts[4] - preRequestCounts[4]);
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      int[] preRequestCounts = getRequestsCounts(servers);
      List<Post> posts = session.query(Post.class).toList();
      assertEquals(6, posts.size());
      int[] postRequestCounts = getRequestsCounts(servers);

      assertEquals(1, postRequestCounts[0] - preRequestCounts[0]);
      assertEquals(1, postRequestCounts[1] - preRequestCounts[1]);
      assertEquals(2, postRequestCounts[2] - preRequestCounts[2]);
      assertEquals(2, postRequestCounts[3] - preRequestCounts[3]);
      assertEquals(2, postRequestCounts[4] - preRequestCounts[4]);
    }
  }
}
