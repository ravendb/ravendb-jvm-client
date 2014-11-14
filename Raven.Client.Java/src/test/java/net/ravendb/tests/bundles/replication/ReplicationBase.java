package net.ravendb.tests.bundles.replication;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDocument;
import net.ravendb.abstractions.replication.ReplicationDestination.TransitiveReplicationOptions;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RavenDBAwareTests;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.listeners.IDocumentConflictListener;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Assert;


public class ReplicationBase extends RavenDBAwareTests {

  private List<IDocumentStore> stores = new ArrayList<>();

  protected int retriesCount = 500;

  public DocumentStore createStore() {
    int port = DEFAULT_SERVER_PORT_1 + stores.size();
    try {
      startServer(port, true);
      createDbAtPort(getDbName(), port);
      DocumentStore store = new DocumentStore("http://" + getHostName() + ":" + port, getDbName());
      store.initialize();
      stores.add(store);
      return store;
    } catch (Exception e) {
      Assert.fail("Couldn't create the store" + e);
    }
    return null;
  }

  public void tellFirstInstanceToReplicateToSecondInstance() {
    tellInstanceToReplicateToAnotherInstance(0, 1);
  }

  public void tellInstanceToReplicateToAnotherInstance(int src, int dest) {
    ReplicationDocument repDoc = createReplicationDocument("http://" + getHostName() + ":"
      + (DEFAULT_SERVER_PORT_1 + dest), getDbName());
    try (IDocumentSession s = stores.get(src).openSession()) {
      s.store(repDoc, "Raven/Replication/Destinations");
      s.saveChanges();
    } catch (Exception e) {
      Assert.fail("Can not add replication document");
    }
  }

  protected void setupReplication(IDatabaseCommands source, DocumentStore... destinations) {
    assertTrue(destinations.length > 0);
    List<RavenJObject> targets = new ArrayList<>();
    for (DocumentStore store : destinations) {
      RavenJObject target = new RavenJObject();
      target.add("Url", store.getUrl());
      target.add("Database", store.getDefaultDatabase());
      target.add("ClientVisibleUrl", store.getUrl());
      targets.add(target);
    }
    setupReplication(source, targets);
  }

  protected void setupReplication(IDatabaseCommands source, String... urls) {
    List<RavenJObject> targets = new ArrayList<>();
    for (String url: urls) {
      RavenJObject target = new RavenJObject();
      target.add("Url", url);
      targets.add(target);
    }
    setupReplication(source, targets);
  }


  protected void setupReplication(IDatabaseCommands source, List<RavenJObject> targets) {
    RavenJObject destinations = new RavenJObject();
    destinations.add("Destinations", new RavenJArray(targets));

    source.put(Constants.RAVEN_REPLICATION_DESTINATIONS, null, destinations, new RavenJObject());
  }

  @After
  public void afterTest() {
    try {
      for (int i = 0; i < stores.size(); i++) {
        stopServer(DEFAULT_SERVER_PORT_1 + i);
      }
    } catch (Exception e) {
      Assert.fail("Can not stop servers");
    }
    stores = new ArrayList<>();
  }

  protected ReplicationDocument createReplicationDocument(String url, String database) {
    ReplicationDestination rep = new ReplicationDestination();
    rep.setUrl(url);
    rep.setDatabase(database);
    rep.setTransitiveReplicationBehavior(TransitiveReplicationOptions.NONE);
    rep.setIgnoredClient(Boolean.FALSE);
    rep.setDisabled(Boolean.FALSE);
    ReplicationDocument repDoc = new ReplicationDocument();
    repDoc.getDestinations().add(rep);
    return repDoc;
  }

  public static class ClientSideConflictResolution implements IDocumentConflictListener {

    @Override
    public boolean tryResolveConflict(String key, List<JsonDocument> conflictedDocs, Reference<JsonDocument> resolvedDocument) {
      RavenJObject dataAsJson = new RavenJObject();

      List<String> items = new ArrayList<>();

      for (JsonDocument conflictedDoc : conflictedDocs) {
        items.add(conflictedDoc.getDataAsJson().value(String.class,"Name"));
      }

      Collections.sort(items);
      String nameJoined = StringUtils.join(items, " ");
      dataAsJson.add("Name", nameJoined);

      resolvedDocument.value = new JsonDocument(dataAsJson, new RavenJObject(), null, null, null, null);
      return true;
    }

  }

}
