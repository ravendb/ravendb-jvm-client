package net.ravendb.client.test.client.spatial;

import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SpatialQueriesTest extends RemoteTestBase {

    private static class SpatialQueriesInMemoryTestIdx extends AbstractIndexCreationTask {
        public SpatialQueriesInMemoryTestIdx() {
            map = "docs.Listings.Select(listingItem => new {\n" +
                    "    classCodes = listingItem.classCodes,\n" +
                    "    latitude = listingItem.latitude,\n" +
                    "    longitude = listingItem.longitude,\n" +
                    "    coordinates = this.CreateSpatialField(((double ? )((double)(listingItem.latitude))), ((double ? )((double)(listingItem.longitude))))\n" +
                    "})";
        }
    }

    @Test
    public void canRunSpatialQueriesInMemory() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new SpatialQueriesInMemoryTestIdx().execute(store);
        }
    }

    private static class Listing {
        private String classCodes;
        private long latitude;
        private long longitude;

        public String getClassCodes() {
            return classCodes;
        }

        public void setClassCodes(String classCodes) {
            this.classCodes = classCodes;
        }

        public long getLatitude() {
            return latitude;
        }

        public void setLatitude(long latitude) {
            this.latitude = latitude;
        }

        public long getLongitude() {
            return longitude;
        }

        public void setLongitude(long longitude) {
            this.longitude = longitude;
        }
    }

    @Test
    public void canSuccessfullyDoSpatialQueryOfNearbyLocations() throws Exception {
        // These items is in a radius of 4 miles (approx 6,5 km)
        DummyGeoDoc areaOneDocOne = new DummyGeoDoc(55.6880508001, 13.5717346673);
        DummyGeoDoc areaOneDocTwo = new DummyGeoDoc(55.6821978456, 13.6076183965);
        DummyGeoDoc areaOneDocThree = new DummyGeoDoc(55.673251569, 13.5946697607);

        // This item is 12 miles (approx 19 km) from the closest in areaOne
        DummyGeoDoc closeButOutsideAreaOne = new DummyGeoDoc(55.8634157297, 13.5497731987);

        // This item is about 3900 miles from areaOne
        DummyGeoDoc newYork = new DummyGeoDoc(40.7137578228, -74.0126901936);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(areaOneDocOne);
                session.store(areaOneDocTwo);
                session.store(areaOneDocThree);
                session.store(closeButOutsideAreaOne);
                session.store(newYork);
                session.saveChanges();

                IndexDefinition indexDefinition = new IndexDefinition();
                indexDefinition.setName("FindByLatLng");
                indexDefinition.setMaps(Sets.newHashSet("from doc in docs select new { coordinates = CreateSpatialField(doc.latitude, doc.longitude) }"));

                store.maintenance().send(new PutIndexesOperation(indexDefinition));

                // Wait until the index is built
                session.query(DummyGeoDoc.class, Query.index("FindByLatLng"))
                        .waitForNonStaleResults()
                        .toList();

                final double lat = 55.6836422426, lng = 13.5871808352; // in the middle of AreaOne
                final double radius = 5.0;

                List<DummyGeoDoc> nearbyDocs = session.query(DummyGeoDoc.class, Query.index("FindByLatLng"))
                        .withinRadiusOf("coordinates", radius, lat, lng)
                        .waitForNonStaleResults()
                        .toList();

                assertThat(nearbyDocs)
                        .isNotNull()
                        .hasSize(3);
            }
        }
    }

    @Test
    public void canSuccessfullyQueryByMiles() throws Exception {
        DummyGeoDoc myHouse = new DummyGeoDoc(44.757767, -93.355322);

        // The gym is about 7.32 miles (11.79 kilometers) from my house.
        DummyGeoDoc gym = new DummyGeoDoc(44.682861, -93.25);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(myHouse);
                session.store(gym);
                session.saveChanges();

                IndexDefinition indexDefinition = new IndexDefinition();
                indexDefinition.setName("FindByLatLng");
                indexDefinition.setMaps(Sets.newHashSet("from doc in docs select new { coordinates = CreateSpatialField(doc.latitude, doc.longitude) }"));

                store.maintenance().send(new PutIndexesOperation(indexDefinition));

                // Wait until the index is built
                session.query(DummyGeoDoc.class, Query.index("FindByLatLng"))
                        .waitForNonStaleResults()
                        .toList();

                final double radius = 8;

                // Find within 8 miles.
                // We should find both my house and the gym.
                List<DummyGeoDoc> matchesWithinMiles = session.query(DummyGeoDoc.class, Query.index("FindByLatLng"))
                        .withinRadiusOf("coordinates", radius, myHouse.getLatitude(), myHouse.getLongitude(), SpatialUnits.MILES)
                        .waitForNonStaleResults()
                        .toList();

                assertThat(matchesWithinMiles)
                        .isNotNull()
                        .hasSize(2);

                // Find within 8 kilometers.
                // We should find only my house, since the gym is ~11 kilometers out.

                List<DummyGeoDoc> matchesWithinKilometers = session.query(DummyGeoDoc.class, Query.index("FindByLatLng"))
                        .withinRadiusOf("coordinates", radius, myHouse.getLatitude(), myHouse.getLongitude(), SpatialUnits.KILOMETERS)
                        .waitForNonStaleResults()
                        .toList();

                assertThat(matchesWithinKilometers)
                        .isNotNull()
                        .hasSize(1);
            }
        }
    }

    public static class DummyGeoDoc {
        private String id;
        private double latitude;
        private double longitude;

        public DummyGeoDoc() {
        }

        public DummyGeoDoc(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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
