package net.ravendb.tests.spatial;

import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.data.StreamResult;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.linq.IRavenQueryable;
import net.ravendb.tests.bugs.User;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class SpatialStreamTest extends RemoteClientTest {


  public static class Location {
    private double lng;
    private double lat;

    public double getLng() {
      return lng;
    }

    public void setLng(double lng) {
      this.lng = lng;
    }

    public double getLat() {
      return lat;
    }

    public void setLat(double lat) {
      this.lat = lat;
    }
  }

  private static void setup(IDocumentStore store) {
    try (IDocumentSession session = store.openSession()) {
      for (int i = 0; i < 500; i++) {
        Location loc1 = new Location();
        loc1.setLat(32);
        loc1.setLng(35);
        session.store(loc1);
      }

      session.saveChanges();
    }
  }

  @Test
  public void canStreamSpatialQuery() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      new LocationsSpatial().execute(store);
      setup(store);

      waitForNonStaleIndexes(store.getDatabaseCommands());

      try (IDocumentSession session = store.openSession()) {

        IRavenQueryable<Location> query = session.query(Location.class, LocationsSpatial.class)
                .customize(new DocumentQueryCustomizationFactory().relatesToShape("WKT", "CIRCLE(35 32 d=10)", SpatialRelation.WITHIN));

        int counter = 0;

        try (CloseableIterator<StreamResult<Location>> results = session.advanced().stream(query)) {
          while (results.hasNext()) {
            StreamResult<Location> streamResult = results.next();
            Assert.assertNotNull(streamResult.getDocument());
            counter++;
          }
        }

        Assert.assertEquals(500, counter);
      }
    }
  }

  public static class LocationsSpatial extends AbstractIndexCreationTask {
    public LocationsSpatial() {
      map = " from l in docs.Locations select new { _ = SpatialGenerate(\"WKT\", l.Lat, l.Lng) } " ;
    }
  }

}
