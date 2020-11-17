package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesRangesCacheTest extends RemoteTestBase {

    @Test
    public void shouldGetTimeSeriesValueFromCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 1), new double[] { 59 }, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get()[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // should load from cache
                val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get()[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void shouldGetPartialRangeFromCache() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 1), new double[]{59}, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get()[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[]{59});
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // should load from cache
                val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addDays(baseLine, 1))[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[]{59});
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                InMemoryDocumentSessionOperations inMemoryDocumentSession = (InMemoryDocumentSessionOperations) session;

                Map<String, List<TimeSeriesRangeResult>> cache = inMemoryDocumentSession.getTimeSeriesByDocId().get("users/ayende");
                assertThat(cache)
                        .isNotNull();

                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .isNotNull()
                        .hasSize(1);
            }
        }
    }

    @Test
    public void shouldGetPartialRangeFromCache2() throws Exception {
        int start = 5;
        int pageSize = 10;

        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 2), 60, "watches/fitbit");
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 3), 61, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addDays(baseLine, 2), DateUtils.addDays(baseLine, 3), start, pageSize);

                assertThat(val)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addDays(baseLine, 1), DateUtils.addDays(baseLine, 4), start, pageSize);

                assertThat(val)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(start, pageSize);

                assertThat(val)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addDays(baseLine, 1), DateUtils.addDays(baseLine, 4), start, pageSize);

                assertThat(val)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests());
            }
        }
    }

    @Test
    public void shouldMergeTimeSeriesRangesInCache() throws Exception {
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
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), new double[] { 6 }, "watches/fitbit");
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

                // should load partial range from cache
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 5), DateUtils.addMinutes(baseLine, 7)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(13);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));
                assertThat(vals.get(12).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 7));

                // should go to server

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 50)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                Map<String, List<TimeSeriesRangeResult>> cache = ((InMemoryDocumentSessionOperations) session).getTimeSeriesByDocId().get("users/ayende");
                assertThat(cache)
                        .isNotNull();
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .isNotNull()
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // should go to server to get [0, 2] and merge it into existing [2, 10]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(31);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(30).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

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

                // should go to server to get [10, 16] and merge it into existing [0, 10]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 16)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(vals)
                        .hasSize(49);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 8));
                assertThat(vals.get(48).getTimestamp())
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

                // should go to server to get range [17, 19]
                // and add it to cache in between [10, 16] and [40, 50]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 17), DateUtils.addMinutes(baseLine, 19)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(vals)
                        .hasSize(13);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 17));
                assertThat(vals.get(12).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 19));

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

                // should go to server to get range [19, 40]
                // and merge the result with existing ranges [17, 19] and [40, 50]
                // into single range [17, 50]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
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

                // should go to server to get range [16, 17]
                // and merge the result with existing ranges [0, 16] and [17, 50]
                // into single range [0, 50]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
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
    public void shouldMergeTimeSeriesRangesInCache2() throws Exception {
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
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 60, "watches/fitbit");
                }

                tsf = session.timeSeriesFor("users/ayende", "Heartrate2");

                tsf.append(DateUtils.addHours(baseLine, 1), 70, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 90), 75, "watches/fitbit");

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

                // should go the server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 22), DateUtils.addMinutes(baseLine, 32)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 22));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 32));

                // should go to server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 1), DateUtils.addMinutes(baseLine, 11)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));

                Map<String, List<TimeSeriesRangeResult>> cache = ((InMemoryDocumentSessionOperations) session).getTimeSeriesByDocId().get("users/ayende");

                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");

                assertThat(ranges)
                        .isNotNull()
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 22));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 32));

                // should go to server to get [32, 35] and merge with [22, 32]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 25), DateUtils.addMinutes(baseLine, 35)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 25));
                assertThat(vals.get(60).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 35));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 22));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 35));

                // should go to server to get [20, 22] and [35, 40]
                // and merge them with [22, 35] into a single range [20, 40]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 20), DateUtils.addMinutes(baseLine, 40)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(vals)
                        .hasSize(121);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 20));
                assertThat(vals.get(120).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 20));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));

                // should go to server to get [15, 20] and merge with [20, 40]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 15), DateUtils.addMinutes(baseLine, 35)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(6);

                assertThat(vals)
                        .hasSize(121);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 15));
                assertThat(vals.get(120).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 35));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 15));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));

                // should go to server and add new cache entry for Heartrate2
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate2")
                        .get(baseLine, DateUtils.addHours(baseLine, 2)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                assertThat(vals)
                        .hasSize(2);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseLine, 1));
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 90));

                List<TimeSeriesRangeResult> ranges2 = cache.get("Heartrate2");
                assertThat(ranges2)
                        .hasSize(1);
                assertThat(ranges2.get(0).getFrom())
                        .isEqualTo(baseLine);
                assertThat(ranges2.get(0).getTo())
                        .isEqualTo(DateUtils.addHours(baseLine, 2));

                // should not go to server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate2")
                        .get(DateUtils.addMinutes(baseLine, 30), DateUtils.addMinutes(baseLine, 100)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                assertThat(vals)
                        .hasSize(2);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addHours(baseLine, 1));
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 90));

                // should go to server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 42), DateUtils.addMinutes(baseLine, 43)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(8);

                assertThat(vals)
                        .hasSize(7);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 42));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 43));

                assertThat(ranges)
                        .hasSize(3);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 11));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 15));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(ranges.get(2).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 42));
                assertThat(ranges.get(2).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 43));

                // should go to server and to get the missing parts and merge all ranges into [0, 45]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(baseLine, DateUtils.addMinutes(baseLine, 45)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(9);

                assertThat(vals)
                        .hasSize(271);

                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(270).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 45));

                ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .isNotNull()
                        .hasSize(1);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 0));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 45));
            }
        }
    }

    @Test
    public void shouldMergeTimeSeriesRangesInCache3() throws Exception {
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
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 60, "watches/fitbit");
                }

                tsf = session.timeSeriesFor("users/ayende", "Heartrate");

                tsf.append(DateUtils.addHours(baseLine, 1), 70, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 90), 75, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 1), DateUtils.addMinutes(baseLine, 2)));

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 5), DateUtils.addMinutes(baseLine, 6)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 6));

                Map<String, List<TimeSeriesRangeResult>> cache = ((InMemoryDocumentSessionOperations) session).getTimeSeriesByDocId().get("users/ayende");
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .isNotNull()
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 6));

                // should go to server to get [2, 3] and merge with [1, 2]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 2), DateUtils.addMinutes(baseLine, 3)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 6));

                // should go to server to get [4, 5] and merge with [5, 6]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 4), DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 4));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 4));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 6));

                // should go to server to get [3, 4] and merge all ranges into [1, 6]

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 4)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 4));

                assertThat(ranges)
                        .hasSize(1);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 6));
            }
        }
    }

    @Test
    public void canHandleRangesWithNoValues() throws Exception {
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
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), 60, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addHours(baseLine, -2), DateUtils.addHours(baseLine, -1)));

                assertThat(vals)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // should not go to server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addHours(baseLine, -2), DateUtils.addHours(baseLine, -1)));

                assertThat(vals)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // should not go to server
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, -90), DateUtils.addMinutes(baseLine, -70)));

                assertThat(vals)
                        .isEmpty();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                // should go to server to get [-60, 1] and merge with [-120, -60]
                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addHours(baseLine, -1), DateUtils.addMinutes(baseLine, 1)));

                assertThat(vals)
                        .hasSize(7);
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals.get(6).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                Map<String, List<TimeSeriesRangeResult>> cache = ((InMemoryDocumentSessionOperations) session).getTimeSeriesByDocId().get("users/ayende");
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");

                assertThat(ranges)
                        .isNotNull()
                        .hasSize(1);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(DateUtils.addHours(baseLine, -2));
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
            }
        }
    }
}
