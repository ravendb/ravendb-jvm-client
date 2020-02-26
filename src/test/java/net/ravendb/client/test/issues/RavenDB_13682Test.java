package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.spatial.PointField;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import net.ravendb.client.infrastructure.entities.Order;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_13682Test extends RemoteTestBase {

    public static class Item {
        private double lat;
        private double lng;
        private String name;

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void canQueryByRoundedSpatialRanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession s = store.openSession()) {
                // 35.1, -106.3 - destination
                Item item1 = new Item();  // 3rd dist - 72.7 km
                item1.setLat(35.1);
                item1.setLng(-107.1);
                item1.setName("a");
                s.store(item1);

                Item item2 = new Item(); // 2nd dist - 64.04 km
                item2.setLat(35.2);
                item2.setLng(-107.0);
                item2.setName("b");
                s.store(item2);

                Item item3 = new Item(); // 1st dist - 28.71 km
                item3.setLat(35.3);
                item3.setLng(-106.5);
                item3.setName("c");
                s.store(item3);

                s.saveChanges();
            }

            try (IDocumentSession s = store.openSession()) {
                // we sort first by spatial distance (but round it up to 25km)
                // then we sort by name ascending, so within 25 range, we can apply a different sort

                List<Item> result = s.advanced().rawQuery(Item.class, "from Items as a " +
                        "order by spatial.distance(spatial.point(a.lat, a.lng), spatial.point(35.1, -106.3), 25), name")
                        .toList();

                assertThat(result)
                        .hasSize(3);

                assertThat(result.get(0).getName())
                        .isEqualTo("c");
                assertThat(result.get(1).getName())
                        .isEqualTo("a");
                assertThat(result.get(2).getName())
                        .isEqualTo("b");
            }

            // dynamic query
            try (IDocumentSession s = store.openSession()) {
                // we sort first by spatial distance (but round it up to 25km)
                // then we sort by name ascending, so within 25 range, we can apply a different sort

                IDocumentQuery<Item> query = s.query(Item.class)
                        .orderByDistance(new PointField("lat", "lng").roundTo(25), 35.1, -106.3);
                List<Item> result = query.toList();

                assertThat(result)
                        .hasSize(3);

                assertThat(result.get(0).getName())
                        .isEqualTo("c");
                assertThat(result.get(1).getName())
                        .isEqualTo("a");
                assertThat(result.get(2).getName())
                        .isEqualTo("b");
            }

            new SpatialIndex().execute(store);
            waitForIndexing(store);

            try (IDocumentSession s = store.openSession()) {
                // we sort first by spatial distance (but round it up to 25km)
                // then we sort by name ascending, so within 25 range, we can apply a different sort

                IDocumentQuery<Item> query = s.query(Item.class, SpatialIndex.class)
                        .orderByDistance("coordinates", 35.1, -106.3, 25);

                List<Item> result = query.toList();

                assertThat(result)
                        .hasSize(3);

                assertThat(result.get(0).getName())
                        .isEqualTo("c");
                assertThat(result.get(1).getName())
                        .isEqualTo("a");
                assertThat(result.get(2).getName())
                        .isEqualTo("b");
            }
        }
    }

    public static class SpatialIndex extends AbstractIndexCreationTask {
        public SpatialIndex() {
            map = "docs.Items.Select(doc => new {\n" +
                    "    name = doc.name, \n" +
                    "    coordinates = this.CreateSpatialField(doc.lat, doc.lng)\n" +
                    "})";
        }
    }

    @Test
    public void canUseDynamicQueryOrderBySpatial_WithAlias() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new CreateSampleDataOperation());

            try (IDocumentSession s = store.openSession()) {
                Order d = s.advanced().rawQuery(Order.class, "from Orders  as a\n" +
                        "order by spatial.distance(\n" +
                        "    spatial.point(a.ShipTo.Location.Latitude, a.ShipTo.Location.Longitude),\n" +
                        "    spatial.point(35.2, -107.2 )\n" +
                        ")\n" +
                        "limit 1")
                        .single();

                IMetadataDictionary metadata = s.advanced().getMetadataFor(d);

                IMetadataDictionary spatial = (IMetadataDictionary)metadata.get("@spatial");
                assertThat((double)spatial.get("Distance"))
                        .isEqualTo(48.99, Offset.offset(0.01));
            }
        }
    }

    @Test
    public void canGetDistanceFromSpatialQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new CreateSampleDataOperation());

            waitForIndexing(store);

            try (IDocumentSession s = store.openSession()) {
                Order d = s.query(Order.class, Query.index("Orders/ByShipment/Location"))
                        .whereEquals("id()", "orders/830-A")
                        .orderByDistance("ShipmentLocation", 35.2, -107.1)
                        .single();

                IMetadataDictionary metadata = s.advanced().getMetadataFor(d);

                IMetadataDictionary spatial = (IMetadataDictionary)metadata.get("@spatial");
                assertThat((double)spatial.get("Distance"))
                        .isEqualTo(40.1, Offset.offset(0.1));
            }
        }
    }
}