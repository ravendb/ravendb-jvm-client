package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeResult;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class TimeSeriesIncludesTest extends RemoteTestBase {

    @Test
    public void sessionLoadWithIncludeTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1-A");

                Order order = new Order();
                order.setCompany("companies/1-A");
                session.store(order, "orders/1-A");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("orders/1-A", "Heartrate");
                tsf.append(baseLine, 67, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 5), 64, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 10), 65, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Order order = session.load(Order.class, "orders/1-A",
                        i -> i.includeDocuments("company")
                                .includeTimeSeries("Heartrate", null, null));

                Company company = session.load(Company.class, order.getCompany());
                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                List<TimeSeriesEntry> values = Arrays.asList(session.timeSeriesFor(order, "Heartrate")
                        .get(null, null));

                assertThat(values)
                        .hasSize(3);

                assertThat(values.get(0).getValues())
                        .hasSize(1);
                assertThat(values.get(0).getValues()[0])
                        .isEqualTo(67);
                assertThat(values.get(0).getTag())
                        .isEqualTo("watches/apple");
                assertThat(values.get(0).getTimestamp())
                        .isEqualTo(baseLine);

                assertThat(values.get(1).getValues())
                        .hasSize(1);
                assertThat(values.get(1).getValues()[0])
                        .isEqualTo(64);
                assertThat(values.get(1).getTag())
                        .isEqualTo("watches/apple");
                assertThat(values.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

                assertThat(values.get(2).getValues())
                        .hasSize(1);
                assertThat(values.get(2).getValues()[0])
                        .isEqualTo(65);
                assertThat(values.get(2).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(values.get(2).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();
            }
        }
    }

    @Test
    public void includeTimeSeriesAndMergeWithExistingRangesInCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "Heartrate");
                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 6, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 2), DateUtils.addMinutes(baseLine, 10)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(49);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(vals.get(48).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                User user = session
                        .load(User.class, documentId,
                                i -> i.includeTimeSeries("Heartrate",
                                        DateUtils.addMinutes(baseLine, 40),
                                        DateUtils.addMinutes(baseLine, 50)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 50)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .hasSize(61);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                InMemoryDocumentSessionOperations sessionOperations = (InMemoryDocumentSessionOperations) session;

                Map<String, List<TimeSeriesRangeResult>> cache = sessionOperations.getTimeSeriesByDocId().get(documentId);
                assertThat(cache)
                        .isNotNull();
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // we intentionally evict just the document (without it's TS data),
                // so that Load request will go to server

                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get [0, 2] and merge it into existing [2, 10]
                user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("Heartrate", baseLine, DateUtils.addMinutes(baseLine, 2)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 2)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(13);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(12).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                assertThat(ranges)
                        .hasSize(2);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // evict just the document
                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get [10, 16] and merge it into existing [0, 10]
                user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("Heartrate", DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 16)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                // should not go to server
                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 16)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(vals)
                        .hasSize(37);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(vals.get(36).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 16));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 16));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // evict just the document
                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get range [17, 19]
                // and add it to cache in between [10, 16] and [40, 50]

                user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("Heartrate",
                                DateUtils.addMinutes(baseLine, 17), DateUtils.addMinutes(baseLine, 19)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 17), DateUtils.addMinutes(baseLine, 19)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(vals)
                        .hasSize(13);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 17));
                assertThat(vals.get(12).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 19));

                assertThat(ranges)
                        .hasSize(3);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 16));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 17));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 19));
                assertThat(ranges.get(2).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(2).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // evict just the document
                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get range [19, 40]
                // and merge the result with existing ranges [17, 19] and [40, 50]
                // into single range [17, 50]

                user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("Heartrate",
                                DateUtils.addMinutes(baseLine, 18), DateUtils.addMinutes(baseLine, 48)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(6);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 18), DateUtils.addMinutes(baseLine, 48)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(6);

                assertThat(vals)
                        .hasSize(181);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 18));
                assertThat(vals.get(180).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 48));

                assertThat(ranges)
                        .hasSize(2);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 16));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 17));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // evict just the document
                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get range [12, 22]
                // and merge the result with existing ranges [0, 16] and [17, 50]
                // into single range [0, 50]

                user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("Heartrate", DateUtils.addMinutes(baseLine, 12), DateUtils.addMinutes(baseLine, 22)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 12), DateUtils.addMinutes(baseLine, 22)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 12));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 22));

                assertThat(ranges)
                        .hasSize(1);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));
            }
        }
    }

    @Test
    public void includeTimeSeriesAndUpdateExistingRangeInCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "Heartrate");
                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 6, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 2), DateUtils.addMinutes(baseLine, 10)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(49);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(vals.get(48).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addSeconds(DateUtils.addMinutes(baseLine, 3), 3), 6, "watches/fitbit");
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                User user = session.load(User.class, "users/ayende",
                        i -> i.includeTimeSeries("Heartrate",
                                DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(14);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addSeconds(DateUtils.addMinutes(baseLine, 3), 3));
                assertThat(vals.get(13).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

            }
        }
    }

    @Test
    public void includeMultipleTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 360; i++) {
                    session.timeSeriesFor("users/ayende", "Heartrate")
                            .append(DateUtils.addSeconds(baseLine, i * 10), 6, "watches/fitbit");
                    session.timeSeriesFor("users/ayende", "BloodPressure")
                            .append(DateUtils.addSeconds(baseLine, i * 10), 66, "watches/fitbit");
                    session.timeSeriesFor("users/ayende", "Nasdaq")
                            .append(DateUtils.addSeconds(baseLine, i * 10), 8097.23, "nasdaq.com");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende",
                        i -> i
                                .includeTimeSeries("Heartrate", DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5))
                                .includeTimeSeries("BloodPressure", DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 45))
                                .includeTimeSeries("Nasdaq", DateUtils.addMinutes(baseLine, 15), DateUtils.addMinutes(baseLine, 25)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(user.getName())
                        .isEqualTo("Oren");

                // should not go to server

                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(13);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(vals.get(12).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "BloodPressure")
                        .get(DateUtils.addMinutes(baseLine, 42), DateUtils.addMinutes(baseLine, 43)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(7);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 42));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 43));

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "BloodPressure")
                        .get(DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 45)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(31);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(vals.get(30).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 45));

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Nasdaq")
                        .get(DateUtils.addMinutes(baseLine, 15), DateUtils.addMinutes(baseLine, 25)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 15));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 25));
            }
        }
    }

    @Test
    public void shouldCacheEmptyTimeSeriesRanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 6, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende",
                        i -> i.includeTimeSeries("Heartrate", DateUtils.addMinutes(baseLine, -30), DateUtils.addMinutes(baseLine, -10)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(user.getName())
                        .isEqualTo("Oren");

                // should not go to server

                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, -30), DateUtils.addMinutes(baseLine, -10)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .isEmpty();

                InMemoryDocumentSessionOperations sessionOperations = (InMemoryDocumentSessionOperations) session;
                Map<String, List<TimeSeriesRangeResult>> cache = sessionOperations.getTimeSeriesByDocId().get("users/ayende");
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .hasSize(1);

                assertThat(ranges.get(0).getEntries())
                        .isEmpty();

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, -30));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, -10));

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, -25), DateUtils.addMinutes(baseLine, -15)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .isEmpty();

                session.advanced().evict(user);

                user = session.load(User.class, "users/ayende",
                        i -> i.includeTimeSeries("BloodPressure",
                                DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "BloodPressure")
                        .get(DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .isEmpty();

                sessionOperations = (InMemoryDocumentSessionOperations) session;

                cache = sessionOperations.getTimeSeriesByDocId().get("users/ayende");
                ranges = cache.get("BloodPRessure");
                assertThat(ranges)
                        .hasSize(1);
                assertThat(ranges.get(0).getEntries())
                        .isEmpty();

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 30));
            }
        }
    }

    @Test
    public void multiLoadWithIncludeTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Oren");
                session.store(user1, "users/ayende");

                User user2 = new User();
                user2.setName("Pawel");
                session.store(user2, "users/ppekrol");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf1 = session.timeSeriesFor("users/ayende", "Heartrate");
                ISessionDocumentTimeSeries tsf2 = session.timeSeriesFor("users/ppekrol", "Heartrate");

                for (int i = 0; i < 360; i++) {
                    tsf1.append(DateUtils.addSeconds(baseLine, i * 10), 6, "watches/fitbit");

                    if (i % 2 == 0) {
                        tsf2.append(DateUtils.addSeconds(baseLine, i * 10), 7, "watches/fitbit");
                    }
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, User> users = session.load(User.class, Arrays.asList("users/ayende", "users/ppekrol"),
                        i -> i.includeTimeSeries("Heartrate", baseLine, DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(users.get("users/ayende").getName())
                        .isEqualTo("Oren");
                assertThat(users.get("users/ppekrol").getName())
                        .isEqualTo("Pawel");

                // should not go to server

                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(181);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(180).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 30));

                // should not go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ppekrol", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(91);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(90).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 30));

            }
        }
    }

    @Test
    public void includeTimeSeriesAndDocumentsAndCounters() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                user.setWorksAt("companies/1");
                session.store(user, "users/ayende");

                Company company = new Company();
                company.setName("HR");
                session.store(company, "companies/1");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");

                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 67, "watches/fitbit");
                }

                session.countersFor("users/ayende").increment("likes", 100);
                session.countersFor("users/ayende").increment("dislikes", 5);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende",
                        i -> i.includeDocuments("worksAt")
                                .includeTimeSeries("Heartrate", baseLine, DateUtils.addMinutes(baseLine, 30))
                                .includeCounter("likes")
                                .includeCounter("dislikes"));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(user.getName())
                        .isEqualTo("Oren");

                // should not go to server

                Company company = session.load(Company.class, user.getWorksAt());
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(company.getName())
                        .isEqualTo("HR");

                // should not go to server
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(181);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getValues()[0])
                        .isEqualTo(67);
                assertThat(vals.get(180).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 30));

                // should not go to server

                Map<String, Long> counters = session.countersFor("users/ayende")
                        .getAll();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                Long counter = counters.get("likes");
                assertThat(counter)
                        .isEqualTo(100);
                counter = counters.get("dislikes");
                assertThat(counter)
                        .isEqualTo(5);
            }
        }
    }

    @Test
    public void queryWithIncludeTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");

                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 67, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.query(User.class)
                        .include(i -> i.includeTimeSeries("Heartrate", null, null));

                List<User> result = query.toList();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(result.get(0).getName())
                        .isEqualTo("Oren");

                // should not go to server

                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 30)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(181);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getValues()[0])
                        .isEqualTo(67);
                assertThat(vals.get(180).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 30));
            }
        }
    }

    public static class User {
        private String name;
        private String worksAt;
        private String id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWorksAt() {
            return worksAt;
        }

        public void setWorksAt(String worksAt) {
            this.worksAt = worksAt;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
