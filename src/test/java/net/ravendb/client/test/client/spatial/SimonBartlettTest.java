package net.ravendb.client.test.client.spatial;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.spatial.SpatialOptions;
import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialSearchStrategy;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SimonBartlettTest extends RemoteTestBase {

    @Test
    public void lineStringsShouldIntersect() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new GeoIndex());

            try (IDocumentSession session = store.openSession()) {
                GeoDocument geoDocument = new GeoDocument();
                geoDocument.setWkt("LINESTRING (0 0, 1 1, 2 1)");
                session.store(geoDocument);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                int count = session.query(Object.class, GeoIndex.class)
                        .spatial("WKT", f -> f.relatesToShape("LINESTRING (1 0, 1 1, 1 2)", SpatialRelation.INTERSECTS))
                        .waitForNonStaleResults()
                        .count();

                assertThat(count)
                        .isEqualTo(1);

                count = session.query(Object.class, GeoIndex.class)
                        .relatesToShape("WKT", "LINESTRING (1 0, 1 1, 1 2)", SpatialRelation.INTERSECTS)
                        .waitForNonStaleResults()
                        .count();

                assertThat(count)
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void circlesShouldNotIntersect() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new GeoIndex());

            try (IDocumentSession session = store.openSession()) {
                // 110km is approximately 1 degree
                GeoDocument geoDocument = new GeoDocument();
                geoDocument.setWkt("CIRCLE(0.000000 0.000000 d=110)");
                session.store(geoDocument);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                // Should not intersect, as there is 1 Degree between the two shapes
                int count = session.query(Object.class, GeoIndex.class)
                        .spatial("WKT", f -> f.relatesToShape("CIRCLE(0.000000 3.000000 d=110)", SpatialRelation.INTERSECTS))
                        .waitForNonStaleResults()
                        .count();

                assertThat(count)
                        .isEqualTo(0);

                count = session.query(Object.class, GeoIndex.class)
                        .relatesToShape("WKT", "CIRCLE(0.000000 3.000000 d=110)", SpatialRelation.INTERSECTS)
                        .waitForNonStaleResults()
                        .count();

                assertThat(count)
                        .isEqualTo(0);
            }
        }
    }

    public static class GeoDocument {
        @JsonProperty("WKT")
        private String wkt;

        public String getWkt() {
            return wkt;
        }

        public void setWkt(String wkt) {
            this.wkt = wkt;
        }
    }

    public static class GeoIndex extends AbstractIndexCreationTask {
        public GeoIndex() {
            map = "docs.GeoDocuments.Select(doc => new {\n" +
                    "    WKT = this.CreateSpatialField(doc.WKT)\n" +
                    "})";
            SpatialOptions spatialOptions = new SpatialOptions();
            spatialOptions.setStrategy(SpatialSearchStrategy.GEOHASH_PREFIX_TREE);
            spatialOptionsStrings.put("WKT", spatialOptions);
        }
    }

}
