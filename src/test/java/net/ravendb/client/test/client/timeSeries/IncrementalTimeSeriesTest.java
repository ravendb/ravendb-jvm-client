package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.Constants;
import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentIncrementalTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.TimeValue;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IncrementalTimeSeriesTest extends RemoteTestBase {

    private final static String INCREMENTAL_TS_NAME = Constants.Headers.INCREMENTAL_TIME_SERIES_PREFIX + "HeartRate";

    private static boolean equals(TimeSeriesEntry entryA, TimeSeriesEntry entryB) {
        if (entryA.getTimestamp().getTime() != entryB.getTimestamp().getTime()) {
            return false;
        }

        if (entryA.getValues().length != entryB.getValues().length) {
            return false;
        }

        for (int i = 0; i < entryA.getValues().length; i++) {
            if (Math.abs(entryA.getValues()[i] - entryB.getValues()[i]) != 0) {
                return false;
            }
        }

        if (!entryA.getTag().equals(entryB.getTag())) {
            return false;
        }

        return entryA.isRollup() == entryB.isRollup();
    }

    @Test
    public void incrementOperationsWithSameTimestampOnDifferentSessionsShouldWork() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, 100_000);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, 100_000);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get(baseline, null);
                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValue())
                        .isEqualTo(200_000);
            }
        }
    }

    @Test
    public void shouldIncrementValueOnEditIncrementalEntry() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, 4);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, 6);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get(baseline, baseline);

                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValue())
                        .isEqualTo(10);
            }
        }
    }

    @Test
    public void getTagForIncremental() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, 4);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get(baseline, baseline);

                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValue())
                        .isEqualTo(4);
                assertThat(ts[0].getTag())
                        .startsWith("TC:INC");
            }
        }
    }

    @Test
    public void shouldIncrementValueOnEditIncrementalEntry2() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 1, 1, 1});
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 0, 0, 9 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME).get(baseline, baseline);
                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValues())
                        .hasSize(3);
                assertThat(ts[0].getValues()[0])
                        .isEqualTo(1);
                assertThat(ts[0].getValues()[1])
                        .isEqualTo(1);
                assertThat(ts[0].getValues()[2])
                        .isEqualTo(10);
            }
        }
    }

    @Test
    public void shouldIncrementValueOnEditIncrementalEntry3() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 1 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 2, 10, 9 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get(baseline, baseline);

                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValues())
                        .hasSize(3);
                assertThat(ts[0].getValues()[0])
                        .isEqualTo(3);
                assertThat(ts[0].getValues()[1])
                        .isEqualTo(10);
                assertThat(ts[0].getValues()[2])
                        .isEqualTo(9);
            }
        }
    }

    @Test
    public void shouldIncrementValueOnEditIncrementalEntry4() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 1, 0 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(DateUtils.addMinutes(baseline, 1), new double[] { 1, -3 , 0, 0 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 0, 0, 0, 0});
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get();

                assertThat(ts)
                        .hasSize(2);
                assertThat(ts[0].getValues())
                        .hasSize(4);
                assertThat(ts[0].getValues()[0])
                        .isEqualTo(1);
                assertThat(ts[0].getValues()[1])
                        .isEqualTo(0);
                assertThat(ts[0].getValues()[2])
                        .isEqualTo(0);
                assertThat(ts[0].getValues()[3])
                        .isEqualTo(0);

                assertThat(ts[1].getValues())
                        .hasSize(4);
                assertThat(ts[1].getValues()[0])
                        .isEqualTo(1);
                assertThat(ts[1].getValues()[1])
                        .isEqualTo(-3);
                assertThat(ts[1].getValues()[2])
                        .isEqualTo(0);
                assertThat(ts[1].getValues()[3])
                        .isEqualTo(0);
            }
        }
    }

    @Test
    public void shouldSplitOperationsIfIncrementContainBothPositiveNegativeValues() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(baseline, new double[] { 1, -2, 3 });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME)
                        .get(baseline, baseline);

                assertThat(ts)
                        .hasSize(1);
                assertThat(ts[0].getValues())
                        .hasSize(3);
                assertThat(ts[0].getValues()[0])
                        .isEqualTo(1);
                assertThat(ts[0].getValues()[1])
                        .isEqualTo(-2);
                assertThat(ts[0].getValues()[2])
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void multipleOperationsOnIncrementalTimeSeries() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                for (int i = 0; i < 10_000; i++) {
                    ts.increment(DateUtils.addMinutes(baseline, i), i);
                }
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME).get();

                assertThat(ts)
                        .hasSize(10_000);
            }
        }
    }

    @Test
    @DisabledOnPullRequest
    public void shouldThrowIfIncrementOperationOnRollupTimeSeries() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            TimeSeriesPolicy p1 = new TimeSeriesPolicy("BySecond", TimeValue.ofSeconds(1));

            TimeSeriesCollectionConfiguration collectionConfig = new TimeSeriesCollectionConfiguration();
            collectionConfig.setPolicies(Arrays.asList(p1));

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            config.setCollections(new HashMap<>());
            config.getCollections().put("Users", collectionConfig);

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));

            Date baseline = DateUtils.addDays(RavenTestHelper.utcToday(), -1);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                for (int i = 0; i < 100; i++) {
                    session.incrementalTimeSeriesFor("users/karmel", INCREMENTAL_TS_NAME)
                            .increment(DateUtils.addSeconds(baseline, 4 * i), new double[] { 29 * i });
                }
                session.saveChanges();
            }

            // wait for rollups to run
            Thread.sleep(1200);

            try (IDocumentSession session = store.openSession()) {
                session.incrementalTimeSeriesFor("users/karmel", INCREMENTAL_TS_NAME)
                        .get();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> {
                    session.incrementalTimeSeriesFor("users/karmel", p1.getTimeSeriesName(INCREMENTAL_TS_NAME)).get();
                }).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Time Series from type Rollup cannot be Incremental");
            }
        }
    }

    @Test
    public void mergeDecAndIncForNodesValues() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            Date time = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(time, 1);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentIncrementalTimeSeries ts = session.incrementalTimeSeriesFor("users/ayende", INCREMENTAL_TS_NAME);
                ts.increment(time, -1);
                session.saveChanges();
            }

            TimeSeriesRangeResult values = store
                    .operations()
                    .send(
                            new GetTimeSeriesOperation("users/ayende", INCREMENTAL_TS_NAME, null, null, 0, 10, null, true));

            assertThat(values.getTotalResults())
                    .isEqualTo(1);
            TimeSeriesEntry result = values.getEntries()[0];
            assertThat(result.getValue())
                    .isEqualTo(0);

            assertThat(result.getNodeValues())
                    .hasSize(1);
        }
    }

    @Test
    public void shouldThrowIfIncrementalTimeSeriesReceiveNameWithoutIncrementalPrefix() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            Date baseline = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");

                session.store(user, "users/ayende");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> {
                    session.incrementalTimeSeriesFor("users/karmel", "Heartrate")
                            .increment(baseline, new double[] { 29.0 });
                    session.saveChanges();
                })
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Time Series name must start with");
            }
        }
    }
}
