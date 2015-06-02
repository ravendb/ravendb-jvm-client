package net.ravendb.tests.shard.blogmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.client.IDocumentSession;


public class MultiLoadOperationsTest extends ShardingScenario {

  @Test
  public void lazyLoadShouldReturnArrayWithNullItems() {
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Lazy<User[]> users = session.advanced().lazily().load(User.class, Arrays.asList("users/1", "users/2"));
      User[] value = users.getValue();
      assertEquals(2, value.length);
      assertNull(value[0]);
      assertNull(value[1]);
    }
  }

  @Test
  public void withLazyQuery() {
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User user1 = new User();
      user1.setId("users/1");
      user1.setName("Yosef Yitzchak Yitzchaki");

      User user2 = new User();
      user2.setId("users/2");
      user2.setName("Fitzchak Yitzchaki");

      session.store(user1);
      session.store(user2);
      session.saveChanges();

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      preRequests = getRequestsCounts(servers);
      Lazy<List<User>> users = session.query(User.class).lazily();
      int[] postRequests = getRequestsCounts(servers);
      for (int i = 0; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }

      List<User> usersUnwrapped = users.getValue();
      assertEquals(2, usersUnwrapped.size());

      postRequests = getRequestsCounts(servers);
      assertEquals(3, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(2, postRequests[i] - preRequests[i]);
      }
    }
  }

  @SuppressWarnings("unused")
  @Test
  public void unlessAccessedLazyOperationsAreNoOp() {
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Lazy<User[]> result1 = session.advanced().lazily().load(User.class, Arrays.asList("users/1", "users/2"));
      Lazy<User[]> result2 = session.advanced().lazily().load(User.class, Arrays.asList("users/3", "users/4"));
      int[] postRequests = getRequestsCounts(servers);
      for (int i = 0; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }
  }

  @Test
  public void lazyOperationsAreBatched() {
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Lazy<User[]> result1 = session.advanced().lazily().load(User.class, Arrays.asList("users/1", "users/2"));
      Lazy<User[]> result2 = session.advanced().lazily().load(User.class, Arrays.asList("users/3", "users/4"));

      assertEquals(2, result2.getValue().length);
      assertEquals(2, result1.getValue().length);

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }
  }

  @Test
  public void lazyMultiLoadOperationWouldBeInTheSession() {
    int[] preRequests = getRequestsCounts(servers);
    List<String> ids = new ArrayList<>();
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      for (int i =1; i<=4; i++) {
        User entity = new User();
        entity.setId("users/" + i);
        entity.setName(ids.isEmpty() ? null : ids.get(ids.size() - 1));
        session.store(entity);
        ids.add(entity.getId());
      }
      session.saveChanges();

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }

    preRequests = getRequestsCounts(servers);

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Lazy<User[]> result1 = session.advanced().lazily().load(User.class, Arrays.asList(ids.get(0), ids.get(1)));
      Lazy<User[]> result2 = session.advanced().lazily().load(User.class, Arrays.asList(ids.get(2), ids.get(3)));

      User[] a = result1.getValue();
      assertEquals(2, a.length);

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }

      User[] b = result2.getValue();
      assertEquals(2, b.length);

      postRequests = getRequestsCounts(servers);
      assertEquals(3, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(2, postRequests[i] - preRequests[i]);
      }

      for (User u : a) {
        assertNotNull(session.advanced().getMetadataFor(u));
      }

      for (User u : b) {
        assertNotNull(session.advanced().getMetadataFor(u));
      }
    }
  }

  @Test
  public void lazyLoadOperationWillHandleIncludes() {
    List<String> ids = new ArrayList<>();
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      for (int i = 1; i <= 4; i++) {
        User entity = new User();
        entity.setName(ids.isEmpty() ? null : ids.get(ids.size() - 1));
        session.store(entity);
        ids.add(entity.getId());
      }

      session.saveChanges();
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      Lazy<User> result1 = session.advanced().lazily()
        .include("Name")
        .load(User.class, ids.get(1));

      Lazy<User> result2 = session.advanced().lazily()
        .include("Name")
        .load(User.class, ids.get(3));

      assertNotNull(result1.getValue());
      assertNotNull(result2.getValue());

      assertTrue(session.advanced().isLoaded(result1.getValue().getName()));
      assertTrue(session.advanced().isLoaded(result2.getValue().getName()));
    }

  }

}
