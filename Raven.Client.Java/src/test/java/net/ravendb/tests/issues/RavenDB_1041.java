package net.ravendb.tests.issues;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeoutException;

import net.ravendb.abstractions.data.PutResult;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.tests.bundles.replication.ReplicationBase;

import org.junit.Test;

public class RavenDB_1041 extends ReplicationBase {

  public static class ReplicatedItem {
    private String id;

    public String getId() {
      return id;
    }
    public void setId(String id) {
      this.id = id;
    }
  }

  @Test
  public void canWaitForReplication() throws TimeoutException {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();
    DocumentStore store3 = createStore();

    setupReplication(store1.getDatabaseCommands(), store2, store3);

    try (IDocumentSession session = store1.openSession()) {
      ReplicatedItem replicatedItem = new ReplicatedItem();
      replicatedItem.setId("Replicated/1");
      session.store(replicatedItem);
      session.saveChanges();
    }

    store1.getReplication().waitSync();
    assertNotNull(store2.getDatabaseCommands().get("Replicated/1"));
    assertNotNull(store3.getDatabaseCommands().get("Replicated/1"));
  }

  @Test
  public void canWaitForReplicationOfParticularEtag() throws TimeoutException {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();
    DocumentStore store3 = createStore();

    setupReplication(store1.getDatabaseCommands(), store2, store3);

    PutResult putResult = store1.getDatabaseCommands().put("Replicated/1", null, new RavenJObject(), new RavenJObject());
    PutResult putResult2 = store1.getDatabaseCommands().put("Replicated/2", null, new RavenJObject(), new RavenJObject());

    store1.getReplication().waitSync(putResult.getEtag(), null, null, 2);

    assertNotNull(store2.getDatabaseCommands().get("Replicated/1"));
    assertNotNull(store3.getDatabaseCommands().get("Replicated/1"));

    store1.getReplication().waitSync(putResult2.getEtag(), null, null, 2);

    assertNotNull(store2.getDatabaseCommands().get("Replicated/2"));
    assertNotNull(store3.getDatabaseCommands().get("Replicated/2"));
  }

  @Test
  public void canSpecifyTimeoutWhenWaitingForReplication() throws Exception {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();
    DocumentStore store3 = createStore();

    setupReplication(store1.getDatabaseCommands(), store2, store3);

    try (IDocumentSession session = store1.openSession()) {
      ReplicatedItem replicatedItem = new ReplicatedItem();
      replicatedItem.setId("Replicated/1");
      session.store(replicatedItem);
      session.saveChanges();
    }

    store1.getReplication().waitSync(null, 20 * 1000L, null, 2);
    assertNotNull(store2.getDatabaseCommands().get("Replicated/1"));
    assertNotNull(store3.getDatabaseCommands().get("Replicated/1"));
  }

  @Test
  public void shouldThrowTimeoutException() {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();

    setupReplication(store1.getDatabaseCommands(), store2.getUrl() + "/databases/" + getDbName(), "http://localhost:1234"); // the last one is not running

    try (IDocumentSession session = store1.openSession()) {
      ReplicatedItem replicatedItem = new ReplicatedItem();
      replicatedItem.setId("Replicated/1");
      session.store(replicatedItem);
      session.saveChanges();
    }

    TimeoutException timeoutException = null;

    try {
      store1.getReplication().waitSync(null, 5000L, null, 2);
    } catch (TimeoutException e) {
      timeoutException = e;
    }

    assertNotNull(timeoutException);
    assertTrue(timeoutException.getMessage().contains("was replicated to 1 of 2 servers"));
  }

  @Test
  public void shouldThrowIfCannotReachEnoughDestinationServers() {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();

    setupReplication(store1.getDatabaseCommands(), store2.getUrl() + "/databases/" + store2.getDatabaseCommands(), "http://localhost:1234", "http://localhost:1235"); // non of them is running

    try (IDocumentSession session = store1.openSession()) {
      ReplicatedItem replicatedItem = new ReplicatedItem();
      replicatedItem.setId("Replicated/1");
      session.store(replicatedItem);
      session.saveChanges();
    }

    try {
      store1.getReplication().waitSync(null, null, null, 3);
      throw new AssertionError("Wait should throw TimeoutException");
    } catch (TimeoutException e) {
      // expected
      assertTrue(e.getMessage().contains("Confirmed that the specified etag"));
    }
  }

  @Test
  public void canWaitForReplicationForOneServerEvenIfTheSecondOneIsDown() throws Exception {
    DocumentStore store1 = createStore();
    DocumentStore store2 = createStore();
    setupReplication(store1.getDatabaseCommands(), store2.getUrl() + "/databases/" + store2.getDefaultDatabase(), "http://localhost:1234"); // the last one is not running

    try (IDocumentSession session = store1.openSession()) {
      ReplicatedItem replicatedItem = new ReplicatedItem();
      replicatedItem.setId("Replicated/1");
      session.store(replicatedItem);
      session.saveChanges();
    }

    store1.getReplication().waitSync(null, null, null, 1);

    assertNotNull(store2.getDatabaseCommands().get("Replicated/1"));
  }
}
