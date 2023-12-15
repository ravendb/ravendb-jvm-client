package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.spatial.PointField;
import net.ravendb.client.documents.queries.spatial.WktField;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_8328Test extends RemoteTestBase {

    @Test
    public void spatialOnAutoIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Item item = new Item();
                item.setLatitude(10);
                item.setLongitude(20);
                item.setLatitude2(10);
                item.setLongitude2(20);
                item.setShapeWkt("POINT(20 10)");
                item.setName("Name1");

                session.store(item);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Item> q = session.query(Item.class)
                        .spatial(new PointField("latitude", "longitude"), f -> f.withinRadius(10, 10, 20));

                IndexQuery iq = q.getIndexQuery();
                assertThat(iq.getQuery())
                        .isEqualTo("from 'Items' where spatial.within(spatial.point(latitude, longitude), spatial.circle($p0, $p1, $p2))");

                q = session.query(Item.class)
                        .spatial(new WktField("shapeWkt"), f -> f.withinRadius(10, 10, 20));

                iq = q.getIndexQuery();
                assertThat(iq.getQuery())
                        .isEqualTo("from 'Items' where spatial.within(spatial.wkt(shapeWkt), spatial.circle($p0, $p1, $p2))");
            }

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<Item> results = session.query(Item.class)
                        .statistics(statsRef)
                        .spatial(new PointField("latitude", "longitude"), f -> f.withinRadius(10, 10, 20))
                        .toList();

                assertThat(results)
                        .hasSize(1);

                assertThat(statsRef.value.getIndexName())
                        .isEqualTo("Auto/Items/BySpatial.point(latitude|longitude)");

                statsRef = new Reference<>();
                results = session.query(Item.class)
                        .statistics(statsRef)
                        .spatial(new PointField("latitude2", "longitude2"), f -> f.withinRadius(10, 10, 20))
                        .toList();

                assertThat(results)
                        .hasSize(1);

                assertThat(statsRef.value.getIndexName())
                        .isEqualTo("Auto/Items/BySpatial.point(latitude|longitude)AndSpatial.point(latitude2|longitude2)");

                statsRef = new Reference<>();
                results = session.query(Item.class)
                        .statistics(statsRef)
                        .spatial(new WktField("shapeWkt"), f -> f.withinRadius(10, 10, 20))
                        .toList();

                assertThat(results)
                        .hasSize(1);

                assertThat(statsRef.value.getIndexName())
                        .isEqualTo("Auto/Items/BySpatial.point(latitude|longitude)AndSpatial.point(latitude2|longitude2)AndSpatial.wkt(shapeWkt)");

            }
        }
    }

    public static class Item {
        private String id;
        private String name;
        private double latitude;
        private double longitude;
        private double latitude2;
        private double longitude2;
        private String shapeWkt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public double getLatitude2() {
            return latitude2;
        }

        public void setLatitude2(double latitude2) {
            this.latitude2 = latitude2;
        }

        public double getLongitude2() {
            return longitude2;
        }

        public void setLongitude2(double longitude2) {
            this.longitude2 = longitude2;
        }

        public String getShapeWkt() {
            return shapeWkt;
        }

        public void setShapeWkt(String shapeWkt) {
            this.shapeWkt = shapeWkt;
        }
    }

}
