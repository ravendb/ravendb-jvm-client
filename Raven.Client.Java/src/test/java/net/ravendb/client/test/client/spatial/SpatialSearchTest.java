package net.ravendb.client.test.client.spatial;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SpatialSearchTest extends RemoteTestBase {
    public static class SpatialIdx extends AbstractIndexCreationTask {
        public SpatialIdx() {
            map = "docs.Events.Select(e => new {\n" +
                    "    capacity = e.capacity,\n" +
                    "    venue = e.venue,\n" +
                    "    date = e.date,\n" +
                    "    coordinates = this.CreateSpatialField(((double ? ) e.latitude), ((double ? ) e.longitude))\n" +
                    "})";

            index("venue", FieldIndexing.SEARCH);
        }
    }

    @Test
    public void can_do_spatial_search_with_client_api() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new SpatialIdx().execute(store);

            try (IDocumentSession session = store.openSession()) {
                session.store(new Event("a/1", 38.9579000, -77.3572000, new Date()));
                session.store(new Event("a/2", 38.9690000, -77.3862000, DateUtils.addDays(new Date(), 1)));
                session.store(new Event("b/2", 38.9690000, -77.3862000, DateUtils.addDays(new Date(), 2)));
                session.store(new Event("c/3", 38.9510000, -77.4107000, DateUtils.addYears(new Date(), 3)));
                session.store(new Event("d/1", 37.9510000, -77.4107000, DateUtils.addYears(new Date(), 3)));
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<Event> events = session.query(Event.class, Query.index("SpatialIdx"))
                        .statistics(statsRef)
                        .whereLessThanOrEqual("date", DateUtils.addYears(new Date(), 1))
                        .withinRadiusOf("coordinates", 6.0, 38.96939, -77.386398)
                        .orderByDescending("date")
                        .toList();

                assertThat(events)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void can_do_spatial_search_with_client_api3() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new SpatialIdx().execute(store);

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Event> matchingVenues = session.advanced().documentQuery(Event.class, SpatialIdx.class)
                        .spatial("coordinates", f -> f.withinRadius(5, 38.9103000, -77.3942))
                        .waitForNonStaleResults();

                IndexQuery iq = matchingVenues.getIndexQuery();

                assertThat(iq.getQuery())
                        .isEqualTo("from index 'SpatialIdx' where spatial.within(coordinates, spatial.circle($p0, $p1, $p2))");

                assertThat(iq.getQueryParameters().get("p0"))
                        .isEqualTo(5.0);
                assertThat(iq.getQueryParameters().get("p1"))
                        .isEqualTo(38.9103);
                assertThat(iq.getQueryParameters().get("p2"))
                        .isEqualTo(-77.3942);
            }
        }
    }

    @Test
    public void can_do_spatial_search_with_client_api_within_given_capacity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new SpatialIdx().execute(store);

            try (IDocumentSession session = store.openSession()) {
                session.store(new Event("a/1", 38.9579000, -77.3572000, new Date(), 5000));
                session.store(new Event("a/2", 38.9690000, -77.3862000, DateUtils.addDays(new Date(), 1), 5000));
                session.store(new Event("b/2", 38.9690000, -77.3862000, DateUtils.addDays(new Date(), 2), 2000));
                session.store(new Event("c/3", 38.9510000, -77.4107000, DateUtils.addYears(new Date(), 3), 1500));
                session.store(new Event("d/1", 37.9510000, -77.4107000, DateUtils.addYears(new Date(), 3), 1500));
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> queryStats = new Reference<>();

                List<Event> events = session.query(Event.class, Query.index("SpatialIdx"))
                        .statistics(queryStats)
                        .openSubclause()
                            .whereGreaterThanOrEqual("capacity", 0)
                            .andAlso()
                            .whereLessThanOrEqual("capacity", 2000)
                        .closeSubclause()
                        .withinRadiusOf("coordinates", 6.0, 38.96939, -77.386398)
                        .orderByDescending("date")
                        .toList();

                assertThat(queryStats.value.getTotalResults())
                        .isEqualTo(2);

                assertThat(events.stream().map(x -> x.getVenue()).collect(Collectors.toList()))
                        .containsExactly("c/3", "b/2");
            }
        }
    }

    @Test
    public void can_do_spatial_search_with_client_api_addorder() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new SpatialIdx().execute(store);

            try (IDocumentSession session = store.openSession()) {
                session.store(new Event("a/1", 38.9579000, -77.3572000));
                session.store(new Event("b/1", 38.9579000, -77.3572000));
                session.store(new Event("c/1", 38.9579000, -77.3572000));
                session.store(new Event("a/2", 38.9690000, -77.3862000));
                session.store(new Event("b/2", 38.9690000, -77.3862000));
                session.store(new Event("c/2", 38.9690000, -77.3862000));
                session.store(new Event("a/3", 38.9510000, -77.4107000));
                session.store(new Event("b/3", 38.9510000, -77.4107000));
                session.store(new Event("c/3", 38.9510000, -77.4107000));
                session.store(new Event("d/1", 37.9510000, -77.4107000));
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<Event> events = session.query(Event.class, Query.index("spatialIdx"))
                        .withinRadiusOf("coordinates", 6.0, 38.96939, -77.386398)
                        .orderByDistance("coordinates", 38.96939, -77.386398)
                        .addOrder("venue", false)
                        .toList();

                assertThat(events.stream().map(x -> x.getVenue()).collect(Collectors.toList()))
                        .containsExactly("a/2", "b/2", "c/2", "a/1", "b/1", "c/1", "a/3", "b/3", "c/3" );
            }

            try (IDocumentSession session = store.openSession()) {
                List<Event> events = session.query(Event.class, Query.index("spatialIdx"))
                        .withinRadiusOf("coordinates", 6.0, 38.96939, -77.386398)
                        .addOrder("venue", false)
                        .orderByDistance("coordinates", 38.96939, -77.386398)
                        .toList();

                assertThat(events.stream().map(x -> x.getVenue()).collect(Collectors.toList()))
                        .containsExactly("a/1", "a/2", "a/3", "b/1", "b/2", "b/3", "c/1", "c/2", "c/3" );
            }
        }
    }

    private static class Event {
        public Event() {
        }

        private String venue;
        private double latitude;
        private double longitude;
        private Date date;
        private int capacity;

        public Event(String venue, double latitude, double longitude) {
            this.venue = venue;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Event(String venue, double latitude, double longitude, Date date) {
            this.venue = venue;
            this.latitude = latitude;
            this.longitude = longitude;
            this.date = date;
        }

        public Event(String venue, double latitude, double longitude, Date date, int capacity) {
            this.venue = venue;
            this.latitude = latitude;
            this.longitude = longitude;
            this.date = date;
            this.capacity = capacity;
        }

        public String getVenue() {
            return venue;
        }

        public void setVenue(String venue) {
            this.venue = venue;
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

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
}
