package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentRollupTypedTimeSeries;
import net.ravendb.client.documents.session.ISessionDocumentTypedTimeSeries;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.test.client.timeSeries.TimeSeriesTypedSessionTest;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RavenDB_16060Test extends RemoteTestBase {

    @Test
    public void canIncludeTypedTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");

                session.store(user, "users/ayende");

                ISessionDocumentTypedTimeSeries<HeartRateMeasure> ts = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende");
                ts.append(baseLine, HeartRateMeasure.create(59), "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> items = session
                        .query(User.class)
                        .include(x -> x.includeTimeSeries("heartRateMeasures"))
                        .toList();

                for (User item : items) {
                    TypedTimeSeriesEntry<HeartRateMeasure>[] timeseries = session.timeSeriesFor(HeartRateMeasure.class, item.getId(), "heartRateMeasures")
                            .get();

                    assertThat(timeseries)
                            .hasSize(1);
                    assertThat(timeseries[0].getValue().getHeartRate())
                            .isEqualTo(59);
                }

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void canServeTimeSeriesFromCache_Typed() throws Exception {
        //RavenDB-16136
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String id = "users/gabor";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Gabor");
                session.store(user, id);

                ISessionDocumentTypedTimeSeries<HeartRateMeasure> ts = session.timeSeriesFor(HeartRateMeasure.class, id);

                ts.append(baseLine, HeartRateMeasure.create(59), "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<HeartRateMeasure>[] timeseries = session.timeSeriesFor(HeartRateMeasure.class, id)
                        .get();

                assertThat(timeseries)
                        .hasSize(1);
                assertThat(timeseries[0].getValue().getHeartRate())
                        .isEqualTo(59);

                TypedTimeSeriesEntry<HeartRateMeasure>[] timeseries2 = session.timeSeriesFor(HeartRateMeasure.class, id)
                        .get();

                assertThat(timeseries2)
                        .hasSize(1);
                assertThat(timeseries2[0].getValue().getHeartRate())
                        .isEqualTo(59);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void includeTimeSeriesAndMergeWithExistingRangesInCache_Typed() throws Exception {
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
                ISessionDocumentTypedTimeSeries<HeartRateMeasure> tsf = session.timeSeriesFor(HeartRateMeasure.class, documentId);

                for (int i = 0; i < 360; i++) {
                    HeartRateMeasure typedMeasure = HeartRateMeasure.create(6);
                    tsf.append(DateUtils.addSeconds(baseLine, i * 10), typedMeasure, "watches/fitibt");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<HeartRateMeasure>[] vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 2), DateUtils.addMinutes(baseLine, 10));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(49);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(vals[48].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                User user = session.load(User.class, documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 50)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 40), DateUtils.addMinutes(baseLine, 50));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 40));
                assertThat(vals[60].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                InMemoryDocumentSessionOperations sessionOperations = (InMemoryDocumentSessionOperations) session;

                Map<String, List<TimeSeriesRangeResult>> cache = sessionOperations.getTimeSeriesByDocId().get(documentId);
                assertThat(cache)
                        .isNotNull();
                List<TimeSeriesRangeResult> ranges = cache.get("heartRateMeasures");
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
                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", baseLine, DateUtils.addMinutes(baseLine, 2)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(baseLine, DateUtils.addMinutes(baseLine, 2));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(13);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(vals[12].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
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
                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 16)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 10), DateUtils.addMinutes(baseLine, 16));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                assertThat(vals)
                        .hasSize(37);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(vals[36].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 16));

                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
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

                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 17), DateUtils.addMinutes(baseLine, 19)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 17), DateUtils.addMinutes(baseLine, 19));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(vals)
                        .hasSize(13);

                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 17));
                assertThat(vals[12].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 19));

                assertThat(ranges)
                        .hasSize(3);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
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

                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 18), DateUtils.addMinutes(baseLine, 48)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(6);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 18), DateUtils.addMinutes(baseLine, 48));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(6);

                assertThat(vals)
                        .hasSize(181);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 18));
                assertThat(vals[180].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 48));

                assertThat(ranges)
                        .hasSize(2);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
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

                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 12), DateUtils.addMinutes(baseLine, 22)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 12), DateUtils.addMinutes(baseLine, 22));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(7);

                assertThat(vals)
                        .hasSize(61);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 12));
                assertThat(vals[60].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 22));

                assertThat(ranges)
                        .hasSize(1);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                // evict just the document
                sessionOperations.documentsByEntity.evict(user);
                sessionOperations.documentsById.remove(documentId);

                // should go to server to get range [50, ∞]
                // and merge the result with existing range [0, 50] into single range [0, ∞]

                user = session.load(User.class,
                        documentId,
                        i -> i.includeTimeSeries("heartRateMeasures", TimeSeriesRangeType.LAST, TimeValue.ofMinutes(10)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(8);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, documentId)
                        .get(DateUtils.addMinutes(baseLine, 50), null);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(8);

                assertThat(vals)
                        .hasSize(60);

                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));
                assertThat(vals[59].getTimestamp())
                        .isEqualTo(DateUtils.addSeconds(DateUtils.addMinutes(baseLine, 59), 50));

                assertThat(ranges)
                        .hasSize(1);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
                assertThat(ranges.get(0).getTo())
                        .isNull();
            }
        }
    }

    @Test
    public void includeTimeSeriesAndUpdateExistingRangeInCache_Typed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTypedTimeSeries<HeartRateMeasure> tsf = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende");

                for (int i = 0; i < 360; i++) {
                    tsf.append(DateUtils.addSeconds(baseLine, i *  10), HeartRateMeasure.create(6), "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<HeartRateMeasure>[] vals = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get(DateUtils.addMinutes(baseLine, 2), DateUtils.addMinutes(baseLine, 10));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                assertThat(vals)
                        .hasSize(49);

                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
                assertThat(vals[48].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));

                session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .append(DateUtils.addSeconds(DateUtils.addMinutes(baseLine, 3), 3),
                                HeartRateMeasure.create(6),
                                "watches/fitbit");
                session.saveChanges();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                User user = session.load(User.class,
                        "users/ayende",
                        i -> i.includeTimeSeries("heartRateMeasures", DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5)));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                // should not go to server

                vals = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get(DateUtils.addMinutes(baseLine, 3), DateUtils.addMinutes(baseLine, 5));

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                assertThat(vals)
                        .hasSize(14);
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
                assertThat(vals[1].getTimestamp())
                        .isEqualTo(DateUtils.addSeconds(DateUtils.addMinutes(baseLine, 3), 3));
                assertThat(vals[13].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 5));
            }
        }
    }

    @Test
    public void canServeTimeSeriesFromCache_Rollup() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RawTimeSeriesPolicy raw = new RawTimeSeriesPolicy(TimeValue.ofHours(24));

            TimeSeriesPolicy p1 = new TimeSeriesPolicy("By6Hours", TimeValue.ofHours(6), TimeValue.ofDays(4));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("By1Day", TimeValue.ofDays(1), TimeValue.ofDays(5));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("By30Minutes", TimeValue.ofMinutes(30), TimeValue.ofDays(2));
            TimeSeriesPolicy p4 = new TimeSeriesPolicy("By1Hour", TimeValue.ofMinutes(60), TimeValue.ofDays(3));

            TimeSeriesCollectionConfiguration timeSeriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            timeSeriesCollectionConfiguration.setRawPolicy(raw);
            timeSeriesCollectionConfiguration.setPolicies(Arrays.asList(p1, p2, p3, p4));

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            config.setCollections(Collections.singletonMap("users", timeSeriesCollectionConfiguration));
            config.setPolicyCheckFrequency(Duration.ofSeconds(1));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));
            store.timeSeries().register(User.class, TimeSeriesTypedSessionTest.StockPrice.class);

            int total = TimeValue.ofDays(12).getValue();
            Date baseLine = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -12);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                ISessionDocumentTypedTimeSeries<TimeSeriesTypedSessionTest.StockPrice> ts = session.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, "users/karmel");
                TimeSeriesTypedSessionTest.StockPrice entry = new TimeSeriesTypedSessionTest.StockPrice();
                for (int i = 0; i <= total; i++) {
                    entry.setOpen(i);
                    entry.setClose(i + 100_000);
                    entry.setHigh(i + 200_000);
                    entry.setLow(i + 300_000);
                    entry.setVolume(i + 400_000);
                    ts.append(DateUtils.addMinutes(baseLine, i), entry, "watches/fibit");
                }
                session.saveChanges();
            }

            Thread.sleep(3000); // wait for rollups

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentRollupTypedTimeSeries<TimeSeriesTypedSessionTest.StockPrice> ts =
                        session.timeSeriesRollupFor(TimeSeriesTypedSessionTest.StockPrice.class, "users/karmel", p1.getName());
                TypedTimeSeriesRollupEntry<TimeSeriesTypedSessionTest.StockPrice>[] res = ts.get();

                assertThat(res)
                        .hasSize(16);

                // should not go to server
                res = ts.get(baseLine, DateUtils.addYears(baseLine, 1));
                assertThat(res)
                        .hasSize(16);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void canIncludeTypedTimeSeries_Rollup() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RawTimeSeriesPolicy raw = new RawTimeSeriesPolicy(TimeValue.ofHours(24));

            TimeSeriesPolicy p1 = new TimeSeriesPolicy("By6Hours", TimeValue.ofHours(6), TimeValue.ofDays(4));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("By1Day", TimeValue.ofDays(1), TimeValue.ofDays(5));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("By30Minutes", TimeValue.ofMinutes(30), TimeValue.ofDays(2));
            TimeSeriesPolicy p4 = new TimeSeriesPolicy("By1Hour", TimeValue.ofMinutes(60), TimeValue.ofDays(3));

            TimeSeriesCollectionConfiguration timeSeriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            timeSeriesCollectionConfiguration.setRawPolicy(raw);
            timeSeriesCollectionConfiguration.setPolicies(Arrays.asList(p1, p2, p3, p4));

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            config.setCollections(Collections.singletonMap("users", timeSeriesCollectionConfiguration));
            config.setPolicyCheckFrequency(Duration.ofSeconds(1));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));
            store.timeSeries().register(User.class, TimeSeriesTypedSessionTest.StockPrice.class);

            int total = TimeValue.ofDays(12).getValue();
            Date baseLine = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -12);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                ISessionDocumentTypedTimeSeries<TimeSeriesTypedSessionTest.StockPrice> ts = session.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, "users/karmel");
                TimeSeriesTypedSessionTest.StockPrice entry = new TimeSeriesTypedSessionTest.StockPrice();
                for (int i = 0; i <= total; i++) {
                    entry.setOpen(i);
                    entry.setClose(i + 100_000);
                    entry.setHigh(i + 200_000);
                    entry.setLow(i + 300_000);
                    entry.setVolume(i + 400_000);
                    ts.append(DateUtils.addMinutes(baseLine, i), entry, "watches/fibit");
                }
                session.saveChanges();
            }

            Thread.sleep(3000); // wait for rollups

            try (IDocumentSession session = store.openSession()) {
                User user = session.query(User.class)
                        .include(i -> i.includeTimeSeries("stockPrices@" + p1.getName()))
                        .first();

                // should not go to server
                TypedTimeSeriesRollupEntry<TimeSeriesTypedSessionTest.StockPrice>[] res = session.timeSeriesRollupFor(TimeSeriesTypedSessionTest.StockPrice.class, user.getId(), p1.getName())
                        .get();

                assertThat(res)
                        .hasSize(16);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }

    public static class HeartRateMeasure {
        @TimeSeriesValue(idx = 0)
        private double heartRate;

        public double getHeartRate() {
            return heartRate;
        }

        public void setHeartRate(double heartRate) {
            this.heartRate = heartRate;
        }

        public static HeartRateMeasure create(double value) {
            HeartRateMeasure measure = new HeartRateMeasure();
            measure.setHeartRate(value);
            return measure;
        }
    }
}
