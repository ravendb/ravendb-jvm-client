package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22959Test extends RemoteTestBase {

    private static final String[] TAGS = new String[]{"watches/apple", "watches/galaxy", "watches/fitbit", "watches/garmin"};

    private static final Instant BASE_TIME = Instant.parse("2024-01-01T01:30:00Z");

    private static final Date FROM = Date.from(BASE_TIME.plus(Duration.ofHours(12)));
    private static final Date TO = Date.from(BASE_TIME.plus(Duration.ofHours(60)));

    @Test
    public void sessionLoadWithIncludeAllTimeSeriesByRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] ids = new String[]{"users/1"};
            storeData(store, ids);

            try (IDocumentSession session = store.openSession()) {
                List<User> queryResults = session.query(User.class)
                        .whereEquals("name", "Shahar")
                        .include(builder -> builder.includeAllTimeSeries(FROM, TO))
                        .toList();

                assertThat(queryResults)
                        .hasSize(1);
                assertRangeEntries(session, queryResults.get(0));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1", builder -> builder.includeAllTimeSeries(FROM, TO));
                assertRangeEntries(session, user);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> queryResults = session.query(User.class)
                        .whereEquals("name", "Shahar")
                        .include(builder -> builder.includeAllTimeSeries())
                        .toList();

                assertThat(queryResults)
                        .hasSize(1);
                assertAllEntries(session, queryResults.get(0));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1", builder -> builder.includeAllTimeSeries());
                assertAllEntries(session, user);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void sessionLoadWithIncludeAllTimeSeriesByRangeWithMultipleDocs() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] ids = new String[]{"users/1", "users/2", "users/3", "users/4"};
            storeData(store, ids);

            try (IDocumentSession session = store.openSession()) {
                List<User> queryResults = session.query(User.class)
                        .whereEquals("name", "Shahar")
                        .include(builder -> builder.includeAllTimeSeries(FROM, TO))
                        .toList();

                assertThat(queryResults)
                        .hasSize(2);
                for (User user : queryResults) {
                    assertRangeEntries(session, user);
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                queryResults = session.query(User.class)
                        .whereEquals("name", "Omer")
                        .toList();

                for (User user : queryResults) {
                    assertRangeEntries(session, user);
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(8);
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> queryResults = session.query(User.class)
                        .whereEquals("name", "Shahar")
                        .include(builder -> builder.includeAllTimeSeries())
                        .toList();

                assertThat(queryResults)
                        .hasSize(2);
                for (User user : queryResults) {
                    assertRangeEntries(session, user);
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                queryResults = session.query(User.class)
                        .whereEquals("name", "Omer")
                        .toList();

                for (User user : queryResults) {
                    assertRangeEntries(session, user);
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(8);
            }
        }
    }

    private static void storeData(IDocumentStore store, String[] ids) {
        try (IDocumentSession session = store.openSession()) {
            for (int i = 0; i < ids.length; i++) {
                User user = new User();
                user.setName(i % 2 == 0 ? "Shahar" : "Omer");
                session.store(user, ids[i]);
                session.saveChanges();
            }

            Instant time = BASE_TIME;
            for (int i = 0; i < TAGS.length; i++) {
                for (int t = 1; t <= 3; t++) {
                    for (String docId : ids) {
                        ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, "TimeSeries" + t);
                        tsf.append(Date.from(time), t * 10 + i, TAGS[i]);
                    }
                }
                session.saveChanges();

                time = time.plus(1, ChronoUnit.DAYS);
            }
        }
    }

    private static void assertRangeEntries(IDocumentSession session, User user) {
        for (int t = 1; t <= 3; t++) {
            TimeSeriesEntry[] entries = session.timeSeriesFor(user, "TimeSeries" + t).get(FROM, TO);
            assertThat(entries)
                    .hasSize(2);
            assertThat(entries[0].getTag())
                    .isEqualTo(TAGS[1]);
            assertThat(entries[1].getTag())
                    .isEqualTo(TAGS[2]);
        }
    }

    private static void assertAllEntries(IDocumentSession session, User user) {
        for (int t = 1; t <= 3; t++) {
            TimeSeriesEntry[] entries = session.timeSeriesFor(user, "TimeSeries" + t).get();
            assertThat(entries)
                    .hasSize(4);
        }
    }
}
