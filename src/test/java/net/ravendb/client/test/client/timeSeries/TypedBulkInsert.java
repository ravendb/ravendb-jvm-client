package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TypedBulkInsert extends RemoteTestBase {

    @Test
    public <TValues> void canCreateSimpleTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(baseline);
                    TimeSeriesTypedSessionTest.HeartRateMeasure measure2 = new TimeSeriesTypedSessionTest.HeartRateMeasure();
                    measure2.setHeartRate(59);
                    measure.setValue(measure2);
                    measure.setTag("watches/fitbit");

                    timeSeriesBulkInsert.append(measure);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> val = session.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")
                        .get()[0];

                assertThat(val.getValue().getHeartRate())
                        .isEqualTo(59);
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(baseline);
            }
        }
    }

    @Test
    public void canCreateSimpleTimeSeries2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");

                bulkInsert.store(user, documentId);

                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                measure.setTimestamp(DateUtils.addMinutes(baseline, 1));
                measure.setValue(new TimeSeriesTypedSessionTest.HeartRateMeasure());
                measure.getValue().setHeartRate(59);
                measure.setTag("watches/fitbit");

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 2), TimeSeriesTypedSessionTest.HeartRateMeasure.create(60), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 2), TimeSeriesTypedSessionTest.HeartRateMeasure.create(61), "watches/fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure>[] val = session.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")
                        .get();

                assertThat(val)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void canCreateTimeSeriesWithoutPassingName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.StockPrice> timeSeriesBulkInsert = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, documentId)) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.StockPrice> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(baseline);
                    TimeSeriesTypedSessionTest.StockPrice stockPrice = new TimeSeriesTypedSessionTest.StockPrice();
                    stockPrice.setClose(1);
                    stockPrice.setOpen(2);
                    stockPrice.setHigh(3);
                    stockPrice.setLow(4);
                    stockPrice.setVolume(55);
                    measure.setValue(stockPrice);
                    measure.setTag("tag");

                    timeSeriesBulkInsert.append(measure);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.StockPrice> val = session.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, documentId)
                        .get()[0];

                assertThat(val.getValue().getClose())
                        .isEqualTo(1);
                assertThat(val.getValue().getOpen())
                        .isEqualTo(2);
                assertThat(val.getValue().getHigh())
                        .isEqualTo(3);
                assertThat(val.getValue().getLow())
                        .isEqualTo(4);
                assertThat(val.getValue().getVolume())
                        .isEqualTo(55);

                assertThat(val.getTag())
                        .isEqualTo("tag");
                assertThat(val.getTimestamp())
                        .isEqualTo(baseline);
            }

            try (IDocumentSession session = store.openSession()) {
                User doc = session.load(User.class, documentId);
                List<String> names = session.advanced().getTimeSeriesFor(doc);

                assertThat(names)
                        .hasSize(1);
                assertThat(names.get(0))
                        .isEqualTo("StockPrices");
            }
        }
    }

    @Test
    public void canDeleteTimestamp() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user);

                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 2), TimeSeriesTypedSessionTest.HeartRateMeasure.create(69), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 3), TimeSeriesTypedSessionTest.HeartRateMeasure.create(79), "watches/fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.timeSeriesFor(documentId, "heartrate")
                        .delete(DateUtils.addMinutes(baseline, 2));

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] vals = session.timeSeriesFor(documentId, "heartrate")
                        .get();

                assertThat(vals)
                        .hasSize(2);
                assertThat(vals[0].getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals[0].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 1));

                assertThat(vals[1].getValues())
                        .isEqualTo(new double[] { 79 });
                assertThat(vals[1].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals[1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 3));
            }
        }
    }

    @Test
    public void usingDifferentTags() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 2), TimeSeriesTypedSessionTest.HeartRateMeasure.create(70), "watches/apple");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] vals = session.timeSeriesFor(documentId, "heartrate")
                        .get();

                assertThat(vals)
                        .hasSize(2);
                assertThat(vals[0].getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals[0].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 1));

                assertThat(vals[1].getValues())
                        .isEqualTo(new double[] { 70 });
                assertThat(vals[1].getTag())
                        .isEqualTo("watches/apple");
                assertThat(vals[1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 2));

            }
        }
    }

    @Test
    public void canStoreAndReadMultipleTimestamps() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> ts = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId)) {
                    ts.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId)) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 2), TimeSeriesTypedSessionTest.HeartRateMeasure.create(61), "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 3), TimeSeriesTypedSessionTest.HeartRateMeasure.create(62), "watches/apple-watch");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure>[] vals = session.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId)
                        .get();

                assertThat(vals)
                        .hasSize(3);

                assertThat(vals[0].getValue().getHeartRate())
                        .isEqualTo(59);
                assertThat(vals[0].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 1));

                assertThat(vals[1].getValue().getHeartRate())
                        .isEqualTo(61);
                assertThat(vals[1].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals[1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 2));

                assertThat(vals[2].getValue().getHeartRate())
                        .isEqualTo(62);
                assertThat(vals[2].getTag())
                        .isEqualTo("watches/apple-watch");
                assertThat(vals[2].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseline, 3));
            }
        }
    }

    @Test
    public void canGetTimeSeriesNames() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId1 = "users/karmel";
            String documentId2 = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                bulkInsert.store(new User(), documentId1);
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.StockPrice> ts =
                             bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, documentId1, "nasdaq2")) {
                    TimeSeriesTypedSessionTest.StockPrice stockPrice = new TimeSeriesTypedSessionTest.StockPrice();
                    stockPrice.setOpen(7547.31);
                    stockPrice.setClose(7123.5);
                    ts.append(new Date(), stockPrice, "web");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> ts =
                             bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId1, "heartrate2")) {
                    TimeSeriesTypedSessionTest.HeartRateMeasure heartRateMeasure = new TimeSeriesTypedSessionTest.HeartRateMeasure();
                    heartRateMeasure.setHeartRate(76);
                    ts.append(new Date(), heartRateMeasure, "watches/apple");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                bulkInsert.store(new User(), documentId2);
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.StockPrice> ts =
                             bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.StockPrice.class, documentId2, "nasdaq")) {
                    TimeSeriesTypedSessionTest.StockPrice stockPrice = new TimeSeriesTypedSessionTest.StockPrice();
                    stockPrice.setOpen(7547.31);
                    stockPrice.setClose(7123.5);
                    ts.append(new Date(), stockPrice, "web");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> ts =
                             bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId2, "heartrate")) {
                    ts.append(new Date(), TimeSeriesTypedSessionTest.HeartRateMeasure.create(58), "fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, documentId2);
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(2);

                // should be sorted
                assertThat(tsNames.get(0))
                        .isEqualTo("heartrate");
                assertThat(tsNames.get(1))
                        .isEqualTo("nasdaq");
            }
        }
    }

    @Test
    public void canStoreAndReadMultipleTimeseriesForDifferentDocuments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = new Date();

            String documentId1 = "users/ayende";
            String documentId2 = "users/grisha";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId1);
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId1, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                }

                User user2 = new User();
                user2.setName("Grisha");
                bulkInsert.store(user2, documentId2);

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId2, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseline, 1), TimeSeriesTypedSessionTest.HeartRateMeasure.create(59), "watches/fitbit");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId1, "heartrate")) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(DateUtils.addMinutes(baseline, 2));
                    measure.setTag("watches/fitbit");
                    measure.setValue(TimeSeriesTypedSessionTest.HeartRateMeasure.create(61));

                    timeSeriesBulkInsert.append(measure);
                }

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId2, "heartrate")) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(DateUtils.addMinutes(baseline, 2));
                    measure.setTag("watches/fitbit");
                    measure.setValue(TimeSeriesTypedSessionTest.HeartRateMeasure.create(61));

                    timeSeriesBulkInsert.append(measure);
                }

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId1, "heartrate")) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(DateUtils.addMinutes(baseline, 3));
                    measure.setTag("watches/apple-watch");
                    measure.setValue(TimeSeriesTypedSessionTest.HeartRateMeasure.create(62));

                    timeSeriesBulkInsert.append(measure);
                }

                try (BulkInsertOperation.TypedTimeSeriesBulkInsert<TimeSeriesTypedSessionTest.HeartRateMeasure> timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId2, "heartrate")) {
                    TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                    measure.setTimestamp(DateUtils.addMinutes(baseline, 3));
                    measure.setTag("watches/apple-watch");
                    measure.setValue(TimeSeriesTypedSessionTest.HeartRateMeasure.create(62));

                    timeSeriesBulkInsert.append(measure);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure>[] vals = session.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId1, "heartrate")
                        .get();

                validateValues(baseline, vals);

                vals = session.timeSeriesFor(TimeSeriesTypedSessionTest.HeartRateMeasure.class, documentId2, "heartrate")
                        .get();

                validateValues(baseline, vals);
            }
        }
    }

    private static void validateValues(Date baseline, TypedTimeSeriesEntry<TimeSeriesTypedSessionTest.HeartRateMeasure>[] vals) {
        assertThat(vals)
                .hasSize(3);

        assertThat(vals[0].getValue().getHeartRate())
                .isEqualTo(59);
        assertThat(vals[0].getTag())
                .isEqualTo("watches/fitbit");
        assertThat(vals[0].getTimestamp())
                .isEqualTo(DateUtils.addMinutes(baseline, 1));

        assertThat(vals[1].getValue().getHeartRate())
                .isEqualTo(61);
        assertThat(vals[1].getTag())
                .isEqualTo("watches/fitbit");
        assertThat(vals[1].getTimestamp())
                .isEqualTo(DateUtils.addMinutes(baseline, 2));

        assertThat(vals[2].getValue().getHeartRate())
                .isEqualTo(62);
        assertThat(vals[2].getTag())
                .isEqualTo("watches/apple-watch");
        assertThat(vals[2].getTimestamp())
                .isEqualTo(DateUtils.addMinutes(baseline, 3));
    }
}
