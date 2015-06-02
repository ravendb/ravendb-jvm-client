package net.ravendb.tests.shard.blogmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

import net.ravendb.client.IDocumentSession;


public class ShardedDocumentSessionIsWorkingTest extends ShardingScenario {

  @Test
  public void multiLoadShouldWork() {
    String id;
    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User u = new User();
      u.setName("Fitzchak Yitzchaki");
      session.store(u);
      id = u.getId();
      session.saveChanges();
    }

    try (IDocumentSession session = shardedDocumentStore.openSession()) {
      User[] users = session.load(User.class, Arrays.asList(id, "does not exists"));
      assertNotNull(users);
      assertEquals(2, users.length);
      assertEquals("Fitzchak Yitzchaki", users[0].getName());
      assertNull(users[1]);
    }
  }
}
