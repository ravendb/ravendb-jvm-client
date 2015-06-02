package net.ravendb.tests.shard.blogmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import net.ravendb.client.IDocumentSession;

import org.junit.Test;

public class CanQueryOnlyUsersTest extends ShardingScenario {

  @Test
  public void whenQueryingForUserById() {
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User user = session.load(User.class, "users/1");
      assertNull(user);
    }
    int[] postRequests = getRequestsCounts(servers);
    assertEquals(2, postRequests[0] - preRequests[0]);
    for (int i = 1; i < postRequests.length; i++) {
      assertEquals(1, postRequests[i] - preRequests[i]);
    }
  }

  @Test
  public void whenQueryingForUsersById() {
    int[] preRequests = getRequestsCounts(servers);

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User[] users = session.load(User.class, Arrays.asList("users/1", "users/2"));
      assertEquals(2, users.length);
      assertNull(users[0]);
      assertNull(users[1]);

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }
  }

  @Test
  public void whenStoringUser() {
    String id;
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User entity = new User();
      entity.setName("Fitzchak Yitzchaki");
      session.store(entity);
      id = entity.getId();

      int[] postRequests = getRequestsCounts(servers);
      assertEquals(3, postRequests[0] - preRequests[0]);

      preRequests = getRequestsCounts(servers);
      session.saveChanges();
      postRequests = getRequestsCounts(servers);
      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }

    preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User user = session.load(User.class, id);
      assertNotNull(user);
      assertEquals("Fitzchak Yitzchaki", user.getName());
      int[] postRequests = getRequestsCounts(servers);

      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }
  }

  @Test
  public void whenQueryingForUserByName() {
    int[] preRequests = getRequestsCounts(servers);
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      QUser u = QUser.user;
      User user = session.query(User.class)
        .firstOrDefault(u.name.eq("Fitzchak"));
      assertNull(user);

      int[] postRequests = getRequestsCounts(servers);

      assertEquals(2, postRequests[0] - preRequests[0]);
      for (int i = 1; i < postRequests.length; i++) {
        assertEquals(1, postRequests[i] - preRequests[i]);
      }
    }
  }
}
