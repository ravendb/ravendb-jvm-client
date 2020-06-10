package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.queries.timeSeries.*;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeSeriesTypedSessionTest extends RemoteTestBase {

    public static class StockPrice {
        @TimeSeriesValue(idx = 0)
        private double open;
        @TimeSeriesValue(idx = 1)
        private double close;
        @TimeSeriesValue(idx = 2)
        private double high;
        @TimeSeriesValue(idx = 3)
        private double low;
        @TimeSeriesValue(idx = 4)
        private double volume;

        public double getOpen() {
            return open;
        }

        public void setOpen(double open) {
            this.open = open;
        }

        public double getClose() {
            return close;
        }

        public void setClose(double close) {
            this.close = close;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
        }
    }

    public static class StockPriceWithBadAttributes {
        @TimeSeriesValue(idx = 1)
        private double open;
        @TimeSeriesValue(idx = 2)
        private double close;
        @TimeSeriesValue(idx = 3)
        private double high;
        @TimeSeriesValue(idx = 4)
        private double low;
        @TimeSeriesValue(idx = 5)
        private double volume;

        public double getOpen() {
            return open;
        }

        public void setOpen(double open) {
            this.open = open;
        }

        public double getClose() {
            return close;
        }

        public void setClose(double close) {
            this.close = close;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
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
    }

    public static class HeartRateMeasureWithCustomName {
        @TimeSeriesValue(idx = 0, name = "HR")
        private double heartRate;

        public double getHeartRate() {
            return heartRate;
        }

        public void setHeartRate(double heartRate) {
            this.heartRate = heartRate;
        }
    }

    @Test
    public void canRegisterTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.timeSeries().register(User.class, StockPrice.class);
            store.timeSeries().register("Users", "HeartRateMeasures", new String[] { "heartRate" });

            TimeSeriesConfiguration updated = store.maintenance().server().send(
                    new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();

            // this method is case insensitive
            String[] heartRate = updated.getNames("users", "HeartRateMeasures");
            assertThat(heartRate)
                    .hasSize(1);
            assertThat(heartRate[0])
                    .isEqualTo("heartRate");

            String[] stock = updated.getNames("users", "StockPrices");
            assertThat(stock)
                    .hasSize(5)
                    .containsExactly("open", "close", "high", "low", "volume");
        }
    }

    @Test
    public void canRegisterTimeSeriesWithCustomName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.timeSeries().register(User.class, HeartRateMeasureWithCustomName.class, "cn");

            TimeSeriesConfiguration updated = store.maintenance().server().send(
                    new GetDatabaseRecordOperation(store.getDatabase())).getTimeSeries();

            // this method is case insensitive
            String[] heartRate = updated.getNames("users", "cn");
            assertThat(heartRate)
                    .hasSize(1);
            assertThat(heartRate[0])
                    .isEqualTo("HR");
        }
    }

    @Test
    public void canCreateSimpleTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");

                HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
                heartRateMeasure.setHeartRate(59);
                ISessionDocumentTypedTimeSeries<HeartRateMeasure> ts =
                        session.timeSeriesFor(HeartRateMeasure.class, "users/ayende");
                ts.append(baseLine, heartRateMeasure, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<HeartRateMeasure> val = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get()[0];

                assertThat(val.getValue().getHeartRate())
                        .isEqualTo(59);
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(baseLine);
            }
        }
    }

    @Test
    public void canCreateSimpleTimeSeriesAsync() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");

                HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
                heartRateMeasure.setHeartRate(59);

                TypedTimeSeriesEntry<HeartRateMeasure> measure = new TypedTimeSeriesEntry<>();
                measure.setTimestamp(DateUtils.addMinutes(baseLine, 1));
                measure.setValue(heartRateMeasure);
                measure.setTag("watches/fitbit");

                ISessionDocumentTypedTimeSeries<HeartRateMeasure> ts = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende");
                ts.append(measure);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TypedTimeSeriesEntry<HeartRateMeasure> val = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get()[0];

                assertThat(val.getValue().getHeartRate())
                        .isEqualTo(59);
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
            }
        }
    }

    @Test
    public void canCreateSimpleTimeSeries2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "HeartRateMeasures");
                tsf.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 60, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TypedTimeSeriesEntry<HeartRateMeasure>> val = Arrays.asList(session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get());
                assertThat(val)
                        .hasSize(2);

                assertThat(val.get(0).getValue().getHeartRate())
                        .isEqualTo(59);
                assertThat(val.get(1).getValue().getHeartRate())
                        .isEqualTo(61);
            }
        }
    }

    @Test
    public void canRequestNonExistingTimeSeriesRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "HeartRateMeasures");
                tsf.append(baseLine, 58, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 10), 60, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TypedTimeSeriesEntry<HeartRateMeasure>> vals = Arrays.asList(session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get(DateUtils.addMinutes(baseLine, -10), DateUtils.addMinutes(baseLine, -5)));

                assertThat(vals)
                        .isEmpty();

                vals = Arrays.asList(session.timeSeriesFor(HeartRateMeasure.class, "users/ayende")
                        .get(DateUtils.addMinutes(baseLine, 5), DateUtils.addMinutes(baseLine, 9)));

                assertThat(vals)
                        .isEmpty();
            }
        }
    }

    @Test
    public void canGetTimeSeriesNames() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/karmel");
                HeartRateMeasure heartRateMeasure = new HeartRateMeasure();
                heartRateMeasure.setHeartRate(66);
                session.timeSeriesFor(HeartRateMeasure.class, "users/karmel")
                        .append(new Date(), heartRateMeasure, "MyHeart");

                StockPrice stockPrice = new StockPrice();
                stockPrice.setOpen(66);
                stockPrice.setClose(55);
                stockPrice.setHigh(113.4);
                stockPrice.setLow(52.4);
                stockPrice.setVolume(15472);
                session.timeSeriesFor(StockPrice.class, "users/karmel")
                        .append(new Date(), stockPrice);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/karmel");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(2);

                // should be sorted
                assertThat(tsNames.get(0))
                        .isEqualTo("HeartRateMeasures");
                assertThat(tsNames.get(1))
                        .isEqualTo("StockPrices");

                TypedTimeSeriesEntry<HeartRateMeasure> heartRateMeasures = session.timeSeriesFor(HeartRateMeasure.class, user)
                        .get()[0];
                assertThat(heartRateMeasures.getValue().getHeartRate())
                        .isEqualTo(66);

                TypedTimeSeriesEntry<StockPrice> stockPriceEntry = session.timeSeriesFor(StockPrice.class, user)
                        .get()[0];
                assertThat(stockPriceEntry.getValue().getOpen())
                        .isEqualTo(66);
                assertThat(stockPriceEntry.getValue().getClose())
                        .isEqualTo(55);
                assertThat(stockPriceEntry.getValue().getHigh())
                        .isEqualTo(113.4);
                assertThat(stockPriceEntry.getValue().getLow())
                        .isEqualTo(52.4);
                assertThat(stockPriceEntry.getValue().getVolume())
                        .isEqualTo(15472);
            }
        }
    }

    @Test
    public void canQueryTimeSeriesAggregation_DeclareSyntax_AllDocsQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");

                ISessionDocumentTypedTimeSeries<HeartRateMeasure> tsf = session.timeSeriesFor(HeartRateMeasure.class, "users/ayende");
                String tag = "watches/fitbit";
                HeartRateMeasure m = new HeartRateMeasure();
                m.setHeartRate(59);

                tsf.append(DateUtils.addMinutes(baseLine, 61), m, tag);

                m.setHeartRate(79);
                tsf.append(DateUtils.addMinutes(baseLine, 62), m, tag);

                m.setHeartRate(69);
                tsf.append(DateUtils.addMinutes(baseLine, 63), m, tag);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesAggregationResult> query =
                        session.advanced().rawQuery(TimeSeriesAggregationResult.class,
                                "declare timeseries out(u)\n" +
                        "    {\n" +
                        "        from u.HeartRateMeasures between $start and $end\n" +
                        "        group by 1h\n" +
                        "        select min(), max(), first(), last()\n" +
                        "    }\n" +
                        "    from @all_docs as u\n" +
                        "    where id() == 'users/ayende'\n" +
                        "    select out(u)")
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addDays(baseLine, 1));

                TypedTimeSeriesAggregationResult<HeartRateMeasure> agg = query.first().asTypedResult(HeartRateMeasure.class);

                assertThat(agg.getCount())
                        .isEqualTo(3);
                assertThat(agg.getResults())
                        .hasSize(1);

                TypedTimeSeriesRangeAggregation<HeartRateMeasure> val = agg.getResults()[0];
                assertThat(val.getFirst().getHeartRate())
                        .isEqualTo(59);
                assertThat(val.getMin().getHeartRate())
                        .isEqualTo(59);

                assertThat(val.getLast().getHeartRate())
                        .isEqualTo(69);
                assertThat(val.getMax().getHeartRate())
                        .isEqualTo(79);

                assertThat(val.getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 60));
                assertThat(val.getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 120));
            }
        }
    }

    @Test
    public void canQueryTimeSeriesAggregation_NoSelectOrGroupBy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 1; i <= 3; i++) {
                    String id = "people/" + i;

                    User user = new User();
                    user.setName("Oren");
                    user.setAge(i * 30);

                    session.store(user, id);

                    ISessionDocumentTypedTimeSeries<HeartRateMeasure> tsf = session.timeSeriesFor(HeartRateMeasure.class, id);
                    HeartRateMeasure m = new HeartRateMeasure();
                    m.setHeartRate(59);

                    tsf.append(DateUtils.addMinutes(baseLine, 61), m, "watches/fitbit");

                    m.setHeartRate(79);
                    tsf.append(DateUtils.addMinutes(baseLine, 62), m,"watches/fitbit");

                    m.setHeartRate(69);
                    tsf.append(DateUtils.addMinutes(baseLine, 63), m, "watches/apple");

                    m.setHeartRate(159);
                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1), m, "watches/fitbit");

                    m.setHeartRate(179);
                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1), m, "watches/apple");

                    m.setHeartRate(169);
                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1), m, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(TimeSeriesRawResult.class, "declare timeseries out(x)\n" +
                        "{\n" +
                        "    from x.HeartRateMeasures between $start and $end\n" +
                        "}\n" +
                        "from Users as doc\n" +
                        "where doc.age > 49\n" +
                        "select out(doc)")
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addMonths(baseLine, 2));

                List<TimeSeriesRawResult> result = query.toList();

                assertThat(result)
                        .hasSize(2);

                for (int i = 0; i < 2; i++) {
                    TimeSeriesRawResult aggRaw = result.get(i);
                    TypedTimeSeriesRawResult<HeartRateMeasure> agg = aggRaw.asTypedResult(HeartRateMeasure.class);

                    assertThat(agg.getResults())
                            .hasSize(6);

                    TypedTimeSeriesEntry<HeartRateMeasure> val = agg.getResults()[0];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(59);
                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 61));

                    val = agg.getResults()[1];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(79);
                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 62));

                    val = agg.getResults()[2];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(69);
                    assertThat(val.getTag())
                            .isEqualTo("watches/apple");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 63));

                    val = agg.getResults()[3];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(159);
                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1));

                    val = agg.getResults()[4];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(179);
                    assertThat(val.getTag())
                            .isEqualTo("watches/apple");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1));

                    val = agg.getResults()[5];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValue().getHeartRate())
                            .isEqualTo(169);
                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1));
                }
            }
        }
    }

    @Test
    public void canWorkWithRollupTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RawTimeSeriesPolicy raw = new RawTimeSeriesPolicy(TimeValue.ofHours(24));
            int rawRetentionSeconds = raw.getRetentionTime().getValue();

            TimeSeriesPolicy p1 = new TimeSeriesPolicy("By6Hours", TimeValue.ofHours(6), TimeValue.ofSeconds(rawRetentionSeconds * 4));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("By1Day", TimeValue.ofDays(1), TimeValue.ofSeconds(rawRetentionSeconds * 5));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("By30Minutes", TimeValue.ofMinutes(30), TimeValue.ofSeconds(rawRetentionSeconds * 2));
            TimeSeriesPolicy p4 = new TimeSeriesPolicy("By1Hour", TimeValue.ofMinutes(60), TimeValue.ofSeconds(rawRetentionSeconds * 3));

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            Map<String, TimeSeriesCollectionConfiguration> collections = new HashMap<>();
            config.setCollections(collections);

            TimeSeriesCollectionConfiguration usersConfig = new TimeSeriesCollectionConfiguration();
            usersConfig.setRawPolicy(raw);
            usersConfig.setPolicies(Arrays.asList(p1, p2, p3, p4));

            collections.put("Users", usersConfig);

            config.setPolicyCheckFrequency(Duration.ofSeconds(1));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));
            store.timeSeries().register(User.class, StockPrice.class);

            // please notice we don't modify server time here!

            Date now = new Date();
            Date baseline = DateUtils.addDays(now, -12);

            int total = (int) Duration.ofDays(12).get(ChronoUnit.SECONDS) / 60;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");
                ISessionDocumentTypedTimeSeries<StockPrice> ts = session.timeSeriesFor(StockPrice.class, "users/karmel");

                StockPrice entry = new StockPrice();

                for (int i = 0; i <= total; i++) {
                    entry.setOpen(i);
                    entry.setClose(i + 100_000);
                    entry.setHigh(i + 200_000);
                    entry.setLow(i + 300_000);
                    entry.setVolume(i + 400_000);
                    ts.append(DateUtils.addMinutes(baseline, i), entry, "watches/fitbit");
                }

                session.saveChanges();
            }

            Thread.sleep(1500); // wait for rollup

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(TimeSeriesRawResult.class, "declare timeseries out()\n" +
                        "{\n" +
                        "    from StockPrices\n" +
                        "    between $start and $end\n" +
                        "}\n" +
                        "from Users as u\n" +
                        "select out()")
                        .addParameter("start", DateUtils.addDays(baseline, -1))
                        .addParameter("end", DateUtils.addDays(now, 1));

                TimeSeriesRawResult resultRaw = query.single();
                TypedTimeSeriesRawResult<StockPrice> result = resultRaw.asTypedResult(StockPrice.class);

                assertThat(result.getResults().length)
                        .isPositive();

                for (TypedTimeSeriesEntry<StockPrice> res : result.getResults()) {

                    if (res.isRollup()) {
                        assertThat(res.getValues().length)
                                .isPositive();
                        assertThat(res.getValue().getLow())
                                .isPositive();
                        assertThat(res.getValue().getHigh())
                                .isPositive();
                    } else {
                        assertThat(res.getValues())
                                .hasSize(5);
                    }
                }
            }

            now = new Date();

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentRollupTypedTimeSeries<StockPrice> ts = session.timeSeriesRollupFor(StockPrice.class, "users/karmel", p1.getName());
                TypedTimeSeriesRollupEntry<StockPrice> a = new TypedTimeSeriesRollupEntry<>(StockPrice.class, new Date());
                a.getMax().setClose(1);
                ts.append(a);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentRollupTypedTimeSeries<StockPrice> ts = session.timeSeriesRollupFor(StockPrice.class, "users/karmel", p1.getName());

                List<TypedTimeSeriesRollupEntry<StockPrice>> res = Arrays.asList(ts.get(DateUtils.addMilliseconds(now, -1), DateUtils.addDays(now, 1)));
                assertThat(res)
                        .hasSize(1);
                assertThat(res.get(0).getMax().getClose())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void mappingNeedsToContainConsecutiveValuesStartingFromZero() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                store.timeSeries().register(Company.class, StockPriceWithBadAttributes.class);
            })
                    .hasMessageContaining("must contain consecutive values starting from 0");
        }
    }
}
