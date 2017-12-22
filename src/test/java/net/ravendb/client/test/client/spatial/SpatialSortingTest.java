package net.ravendb.client.test.client.spatial;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexFieldOptions;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentSession;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SpatialSortingTest extends RemoteTestBase {

    private final static double FILTERED_LAT = 44.419575;
    private final static double FILTERED_LNG = 34.042618;
    private final static double SORTED_LAT = 44.417398;
    private final static double SORTED_LNG = 34.042575;
    private final static double FILTERED_RADIUS = 100;

    public static class Shop {
        private String id;
        private double latitude;
        private double longitude;

        public Shop() {
        }

        public Shop(double latitude, double longitude) {
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

    private final Shop[] shops = new Shop[] {
            new Shop(44.420678, 34.042490),
            new Shop(44.419712, 34.042232),
            new Shop(44.418686, 34.043219)
    };

    //shop/1:0.36KM, shop/2:0.26KM, shop/3 0.15KM from (34.042575,  44.417398)
    private final static String[] sortedExpectedOrder = new String[] { "shops/3-A", "shops/2-A", "shops/1-A" };

    //shop/1:0.12KM, shop/2:0.03KM, shop/3 0.11KM from (34.042618,  44.419575)
    private final static String[] filteredExpectedOrder = new String[] { "shops/2-A", "shops/3-A", "shops/1-A"  };

    public void createData(IDocumentStore store) {
        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName("eventsByLatLng");
        indexDefinition.setMaps(Collections.singleton("from e in docs.Shops select new { e.venue, coordinates = CreateSpatialField(e.latitude, e.longitude) }"));

        Map<String, IndexFieldOptions> fields = new HashMap<>();
        IndexFieldOptions options = new IndexFieldOptions();
        options.setIndexing(FieldIndexing.EXACT);
        fields.put("tag", options);
        indexDefinition.setFields(fields);

        store.maintenance().send(new PutIndexesOperation(indexDefinition));

        IndexDefinition indexDefinition2 = new IndexDefinition();
        indexDefinition2.setName("eventsByLatLngWSpecialField");
        indexDefinition2.setMaps(Collections.singleton("from e in docs.Shops select new { e.venue, mySpacialField = CreateSpatialField(e.latitude, e.longitude) }"));

        IndexFieldOptions indexFieldOptions = new IndexFieldOptions();
        indexFieldOptions.setIndexing(FieldIndexing.EXACT);
        indexDefinition2.setFields(Collections.singletonMap("tag", indexFieldOptions));

        store.maintenance().send(new PutIndexesOperation(indexDefinition2));

        try (IDocumentSession session = store.openSession()) {
            for (Shop shop : shops) {
                session.store(shop);
            }
            session.saveChanges();
        }

        waitForIndexing(store);
    }

    private static void assertResultsOrder(String[] resultIDs, String[] expectedOrder) {
        assertThat(resultIDs)
                .containsExactly(expectedOrder);
    }

    @Test
    public void canFilterByLocationAndSortByDistanceFromDifferentPointWDocQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            createData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLng"))
                        .spatial("coordinates", f -> f.within(getQueryShapeFromLatLon(FILTERED_LAT, FILTERED_LNG, FILTERED_RADIUS)))
                        .orderByDistance("coordinates", SORTED_LAT, SORTED_LNG)
                        .toList();

                assertResultsOrder(shops.stream().map(x -> x.getId()).toArray(String[]::new), sortedExpectedOrder);
            }
        }
    }

    @Test
    public void canSortByDistanceWOFilteringWDocQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            createData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLng"))
                        .orderByDistance("coordinates", SORTED_LAT, SORTED_LNG)
                        .toList();

                assertResultsOrder(shops.stream().map(x -> x.getId()).toArray(String[]::new), sortedExpectedOrder);
            }
        }
    }

    @Test
    public void canSortByDistanceWOFilteringWDocQueryBySpecifiedField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            createData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLngWSpecialField"))
                        .orderByDistance("mySpacialField", SORTED_LAT, SORTED_LNG)
                        .toList();

                assertResultsOrder(shops.stream().map(x -> x.getId()).toArray(String[]::new), sortedExpectedOrder);
            }
        }
    }

    @Test
    public void canSortByDistanceWOFiltering() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            createData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLng"))
                        .orderByDistance("coordinates", FILTERED_LAT, FILTERED_LNG)
                        .toList();

                assertResultsOrder(shops.stream().map(x -> x.getId()).toArray(String[]::new), filteredExpectedOrder);
            }

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLng"))
                        .orderByDistanceDescending("coordinates", FILTERED_LAT, FILTERED_LNG)
                        .toList();

                String[] strings = shops.stream().map(x -> x.getId()).toArray(String[]::new);
                ArrayUtils.reverse(strings);
                assertResultsOrder(strings, filteredExpectedOrder);
            }
        }
    }

    @Test
    public void canSortByDistanceWOFilteringBySpecifiedField() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            createData(store);

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLngWSpecialField"))
                        .orderByDistance("mySpacialField", FILTERED_LAT, FILTERED_LNG)
                        .toList();

                assertResultsOrder(shops.stream().map(x -> x.getId()).toArray(String[]::new), filteredExpectedOrder);
            }

            try (IDocumentSession session = store.openSession()) {
                List<Shop> shops = session.query(Shop.class, Query.index("eventsByLatLngWSpecialField"))
                        .orderByDistanceDescending("mySpacialField", FILTERED_LAT, FILTERED_LNG)
                        .toList();

                String[] strings = shops.stream().map(x -> x.getId()).toArray(String[]::new);
                ArrayUtils.reverse(strings);
                assertResultsOrder(strings, filteredExpectedOrder);
            }
        }
    }

    private static String getQueryShapeFromLatLon(double lat, double lng, double radius) {
        return "Circle(" + lng + " " + lat + " d=" + radius + ")";
    }
}
