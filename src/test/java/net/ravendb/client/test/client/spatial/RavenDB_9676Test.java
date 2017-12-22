package net.ravendb.client.test.client.spatial;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.spatial.PointField;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_9676Test extends RemoteTestBase {

    @Test
    public void canOrderByDistanceOnDynamicSpatialField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Item item = new Item();
                item.setName("Item1");
                item.setLatitude(10);
                item.setLongitude(10);

                session.store(item);

                Item item1 = new Item();
                item1.setName("Item2");
                item1.setLatitude(11);
                item1.setLongitude(11);

                session.store(item1);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<Item> items = session.query(Item.class)
                        .waitForNonStaleResults()
                        .spatial(new PointField("latitude", "longitude"), f -> f.withinRadius(1000, 10, 10))
                        .orderByDistance(new PointField("latitude", "longitude"), 10, 10)
                        .toList();

                assertThat(items)
                        .hasSize(2);

                assertThat(items.get(0).getName())
                        .isEqualTo("Item1");

                assertThat(items.get(1).getName())
                        .isEqualTo("Item2");

                items = session.query(Item.class)
                        .waitForNonStaleResults()
                        .spatial(new PointField("latitude", "longitude"), f -> f.withinRadius(1000, 10, 10))
                        .orderByDistanceDescending(new PointField("latitude", "longitude"), 10, 10)
                        .toList();

                assertThat(items)
                        .hasSize(2);

                assertThat(items.get(0).getName())
                        .isEqualTo("Item2");

                assertThat(items.get(1).getName())
                        .isEqualTo("Item1");
            }
        }
    }

    public static class Item {
        private String name;
        private double latitude;
        private double longitude;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }
}
