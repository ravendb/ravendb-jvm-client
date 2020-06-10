package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.TimeValue;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesSessionTest extends RemoteTestBase {

    @Test
    public void canCreateSimpleTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry val = session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null)[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[] { 59 });
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
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 60, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 3), 61, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> val = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));
                assertThat(val)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void canDeleteTimestamp() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 69, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 3), 79, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .remove(DateUtils.addMinutes(baseLine, 2));

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(2);

                assertThat(vals.get(0).getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(vals.get(1).getValues())
                        .isEqualTo(new double[] { 79 });
                assertThat(vals.get(1).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
            }
        }
    }

    @Test
    public void usingDifferentTags() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 70, "watches/apple");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(2);
                assertThat(vals.get(0).getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(vals.get(1).getValues())
                        .isEqualTo(new double[] { 70 });
                assertThat(vals.get(1).getTag())
                        .isEqualTo("watches/apple");
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));
            }
        }
    }

    @Test
    public void usingDifferentNumberOfValues_SmallToLarge() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), new double[] { 70, 120, 80 }, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 3), 69, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(3);
                assertThat(vals.get(0).getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(vals.get(1).getValues())
                        .isEqualTo(new double[] { 70, 120, 80 });
                assertThat(vals.get(1).getTag())
                        .isEqualTo("watches/apple");
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                assertThat(vals.get(2).getValues())
                        .isEqualTo(new double[] { 69 });
                assertThat(vals.get(2).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(2).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
            }
        }
    }

    @Test
    public void usingDifferentNumberOfValues_LargeToSmall() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), new double[]{ 70, 120, 80}, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 3), 69, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(3);

                assertThat(vals.get(0).getValues())
                        .isEqualTo(new double[] { 70, 120, 80 });
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/apple");
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(vals.get(1).getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals.get(1).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                assertThat(vals.get(2).getValues())
                        .isEqualTo(new double[] { 69 });
                assertThat(vals.get(2).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(2).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
            }
        }
    }

    @Test
    public void canStoreAndReadMultipleTimestamps() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), new double[]{ 59 }, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 3), 62, "watches/apple-watch");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(3);

                assertThat(vals.get(0).getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(vals.get(0).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(0).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));

                assertThat(vals.get(1).getValues())
                        .isEqualTo(new double[] { 61 });
                assertThat(vals.get(1).getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(vals.get(1).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 2));

                assertThat(vals.get(2).getValues())
                        .isEqualTo(new double[] { 62 });
                assertThat(vals.get(2).getTag())
                        .isEqualTo("watches/apple-watch");
                assertThat(vals.get(2).getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 3));
            }
        }
    }

    @Test
    public void canStoreLargeNumberOfValues() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            int offset = 0;

            for (int i = 0; i < 10; i++) {
                try (IDocumentSession session = store.openSession()) {
                    ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                    for (int j = 0; j < 1000; j++) {
                        tsf.append(DateUtils.addMinutes(baseLine, offset++), new double[] { offset }, "watches/fitbit");
                    }

                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));
                assertThat(vals)
                        .hasSize(10_000);

                for (int i = 0; i < 10_000; i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, i));
                    assertThat(vals.get(i).getValues()[0])
                            .isEqualTo(1 + i);
                }
            }
        }
    }

    @Test
    public void canStoreValuesOutOfOrder() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            final int retries = 1000;

            int offset = 0;

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                for (int j = 0; j < retries; j++) {

                    tsf.append(DateUtils.addMinutes(baseLine, offset), new double[] { offset }, "watches/fitbit");

                    offset += 5;
                }

                session.saveChanges();
            }

            offset = 1;

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                List<TimeSeriesEntry> vals = Arrays.asList(tsf.get(null, null));
                assertThat(vals)
                        .hasSize(retries);

                for (int j = 0; j < retries; j++) {
                    tsf.append(DateUtils.addMinutes(baseLine, offset), new double[] { offset }, "watches/fitbit");
                    offset += 5;
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(2 * retries);

                offset = 0;
                for (int i = 0; i < retries; i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, offset));
                    assertThat(vals.get(i).getValues()[0])
                            .isEqualTo(offset);

                    offset++;
                    i++;

                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, offset));
                    assertThat(vals.get(i).getValues()[0])
                            .isEqualTo(offset);

                    offset += 4;
                }
            }
        }
    }

    @Test
    public void sessionGetShouldIncludeValuesFromRollUpsInResult() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RawTimeSeriesPolicy raw = new RawTimeSeriesPolicy(TimeValue.ofHours(24));

            TimeSeriesPolicy p1 = new TimeSeriesPolicy("By6Hours", TimeValue.ofHours(6), TimeValue.ofSeconds(raw.getRetentionTime().getValue() * 4));
            TimeSeriesPolicy p2 = new TimeSeriesPolicy("By1Day", TimeValue.ofDays(1), TimeValue.ofSeconds(raw.getRetentionTime().getValue() * 5));
            TimeSeriesPolicy p3 = new TimeSeriesPolicy("By30Minutes", TimeValue.ofMinutes(30), TimeValue.ofSeconds(raw.getRetentionTime().getValue() * 2));
            TimeSeriesPolicy p4 = new TimeSeriesPolicy("By1Hour", TimeValue.ofMinutes(60), TimeValue.ofSeconds(raw.getRetentionTime().getValue() * 3));

            TimeSeriesCollectionConfiguration seriesCollectionConfiguration = new TimeSeriesCollectionConfiguration();
            seriesCollectionConfiguration.setRawPolicy(raw);
            seriesCollectionConfiguration.setPolicies(Arrays.asList(p1, p2, p3, p4));

            Map<String, TimeSeriesCollectionConfiguration> collectionsConfig = new HashMap<>();
            collectionsConfig.put("Users", seriesCollectionConfiguration);

            TimeSeriesConfiguration config = new TimeSeriesConfiguration();
            config.setCollections(collectionsConfig);
            config.setPolicyCheckFrequency(Duration.ofSeconds(1));

            store.maintenance().send(new ConfigureTimeSeriesOperation(config));


            Date now = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            Date baseline = DateUtils.addDays(now, -12);

            long total = Duration.ofDays(12).getSeconds() / 60;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                for (long i = 0; i <= total; i++) {
                    session.timeSeriesFor("users/karmel", "Heartrate")
                            .append(DateUtils.addMinutes(baseline, (int)i), i, "watches/fitbit");
                }

                session.saveChanges();
            }


            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> result = Arrays.asList(session.timeSeriesFor("users/karmel", "Heartrate")
                        .get());

                assertThat(result.size())
                        .isPositive();
            }
        }
    }

    @Test
    public void canRequestNonExistingTimeSeriesRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(baseLine, 58, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 10), 60, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, -10), DateUtils.addMinutes(baseLine, -5)));

                assertThat(vals)
                        .isEmpty();

                vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(DateUtils.addMinutes(baseLine, 5), DateUtils.addMinutes(baseLine, 9)));

                assertThat(vals)
                        .isEmpty();
            }
        }
    }

    @Test
    public void canGetTimeSeriesNames() throws Exception {
        Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/karmel");
                session.timeSeriesFor("users/karmel", "Nasdaq2")
                        .append(new Date(), 7547.31, "web");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.timeSeriesFor("users/karmel", "Heartrate2")
                        .append(new Date(), 7547.31, "web");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/ayende");
                session.timeSeriesFor("users/ayende", "Nasdaq")
                        .append(new Date(), 7547.31, "web");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.timeSeriesFor("users/ayende", "Heartrate")
                        .append(DateUtils.addMinutes(new Date(), 1), 58, "fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(2);

                // should be sorted
                assertThat(tsNames.get(0))
                        .isEqualTo("Heartrate");
                assertThat(tsNames.get(1))
                        .isEqualTo("Nasdaq");
            }

            try (IDocumentSession session = store.openSession()) {
                session.timeSeriesFor("users/ayende", "heartrate")  // putting ts name as lower cased
                    .append(DateUtils.addMinutes(baseLine, 1), 58, "fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(2);

                // should preserve original casing
                assertThat(tsNames.get(0))
                        .isEqualTo("Heartrate");
                assertThat(tsNames.get(1))
                        .isEqualTo("Nasdaq");
            }
        }
    }

    @Test
    public void canGetTimeSeriesNames2() throws Exception {
        Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            int offset = 0;

            for (int i = 0; i < 100; i++) {
                try (IDocumentSession sessions = store.openSession()) {
                    ISessionDocumentTimeSeries tsf = sessions.timeSeriesFor("users/ayende", "Heartrate");

                    for (int j = 0; j < 1000; j++) {
                        tsf.append(DateUtils.addMinutes(baseLine, offset++), offset, "watches/fitbit");
                    }

                    sessions.saveChanges();
                }
            }

            offset = 0;

            for (int i = 0; i < 100; i++) {
                try (IDocumentSession session = store.openSession()) {
                    ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Pulse");

                    for (int j = 0; j < 1000; j++) {
                        tsf.append(DateUtils.addMinutes(baseLine, offset++), offset, "watches/fitbit");
                    }
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null));

                assertThat(vals)
                        .hasSize(100_000);

                for (int i = 0; i < 100_000; i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, i));
                    assertThat(vals.get(i).getValues()[0])
                            .isEqualTo(1 + i);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Pulse")
                        .get(null, null));
                assertThat(vals)
                        .hasSize(100_000);

                for (int i = 0; i < 100_000; i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, i));
                    assertThat(vals.get(i).getValue())
                            .isEqualTo(1 + i);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);

                // should be sorted
                assertThat(tsNames.get(0))
                        .isEqualTo("Heartrate");
                assertThat(tsNames.get(1))
                        .isEqualTo("Pulse");
            }

        }
    }

    @Test
    public void shouldDeleteTimeSeriesUponDocumentDeletion() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String id = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, id);

                ISessionDocumentTimeSeries timeSeriesFor = session.timeSeriesFor(id, "Heartrate");
                timeSeriesFor.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                timeSeriesFor.append(DateUtils.addMinutes(baseLine, 2), 59, "watches/fitbit");

                session.timeSeriesFor(id, "Heartrate2")
                        .append(DateUtils.addMinutes(baseLine, 1), 59, "watches/apple");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete(id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] vals = session.timeSeriesFor(id, "Heartrate")
                        .get(null, null);
                assertThat(vals)
                        .isNull();

                vals = session.timeSeriesFor(id, "Heartrate2")
                        .get(null, null);
                assertThat(vals)
                        .isNull();
            }
        }
    }

    @Test
    public void canSkipAndTakeTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");

                for (int i = 0; i < 100; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), 100 + i, "watches/fitbit");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor("users/ayende", "Heartrate")
                        .get(null, null, 5, 20));

                assertThat(vals)
                        .hasSize(20);

                for (int i = 0; i < vals.size(); i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 5 + i));
                    assertThat(vals.get(i).getValue())
                            .isEqualTo(105 + i);
                }
            }
        }
    }

    @Test
    public void shouldEvictTimeSeriesUponEntityEviction() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");

                session.store(user, documentId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "Heartrate");

                for (int i = 0; i < 60; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), 100 + i, "watches/fitbit");
                }

                tsf = session.timeSeriesFor(documentId, "BloodPressure");

                for (int i = 0; i < 10; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { 120 - i, 80 + i}, "watches/apple");
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, documentId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(user, "Heartrate");

                List<TimeSeriesEntry> vals = Arrays.asList(tsf.get(baseLine, DateUtils.addMinutes(baseLine, 10)));

                assertThat(vals)
                        .hasSize(11);

                vals = Arrays.asList(tsf.get(DateUtils.addMinutes(baseLine, 20), DateUtils.addMinutes(baseLine, 50)));

                assertThat(vals)
                        .hasSize(31);

                tsf = session.timeSeriesFor(user, "BloodPressure");

                vals = Arrays.asList(tsf.get());

                assertThat(vals)
                        .hasSize(10);

                InMemoryDocumentSessionOperations sessionOperations = (InMemoryDocumentSessionOperations) session;

                assertThat(sessionOperations.getTimeSeriesByDocId())
                        .hasSize(1);
                Map<String, List<TimeSeriesRangeResult>> cache = sessionOperations.getTimeSeriesByDocId().get(documentId);
                assertThat(cache)
                        .hasSize(2);
                List<TimeSeriesRangeResult> ranges = cache.get("Heartrate");
                assertThat(ranges)
                        .hasSize(2);

                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(baseLine);
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(ranges.get(0).getEntries())
                        .hasSize(11);
                assertThat(ranges.get(1).getFrom())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 20));
                assertThat(ranges.get(1).getTo())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 50));

                assertThat(ranges.get(1).getEntries())
                        .hasSize(31);
                ranges = cache.get("BloodPressure");
                assertThat(ranges)
                        .hasSize(1);
                assertThat(ranges.get(0).getFrom())
                        .isEqualTo(NetISO8601Utils.MIN_DATE);
                assertThat(ranges.get(0).getTo())
                        .isEqualTo(NetISO8601Utils.MAX_DATE);
                assertThat(ranges.get(0).getEntries())
                        .hasSize(10);

                session.advanced().evict(user);

                cache = sessionOperations.getTimeSeriesByDocId().get(documentId);
                assertThat(cache)
                        .isNull();
                assertThat(sessionOperations.getTimeSeriesByDocId())
                        .isEmpty();
            }
        }
    }

    @Test
    public void getAllTimeSeriesNamesWhenNoTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/karmel");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/karmel");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .isEmpty();
            }
        }
    }

    @Test
    public void getSingleTimeSeriesWhenNoTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/karmel");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/karmel");
                TimeSeriesEntry[] ts = session.timeSeriesFor(user, "unicorns")
                        .get();
                assertThat(ts)
                        .isNull();
            }
        }
    }

    @Test
    public void canDeleteWithoutProvidingFromAndToDates() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            String docId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), docId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, "HeartRate");
                ISessionDocumentTimeSeries tsf2 = session.timeSeriesFor(docId, "BloodPressure");
                ISessionDocumentTimeSeries tsf3 = session.timeSeriesFor(docId, "BodyTemperature");

                for (int j = 0; j < 100; j++) {
                    tsf.append(DateUtils.addMinutes(baseLine, j), j);
                    tsf2.append(DateUtils.addMinutes(baseLine, j), j);
                    tsf3.append(DateUtils.addMinutes(baseLine, j), j);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, "Heartrate");

                List<TimeSeriesEntry> entries = Arrays.asList(tsf.get());
                assertThat(entries)
                        .hasSize(100);

                // null From, To
                tsf.remove();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] entries = session.timeSeriesFor(docId, "Heartrate").get();
                assertThat(entries)
                        .isNull();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, "BloodPressure");

                List<TimeSeriesEntry> entries = Arrays.asList(tsf.get());
                assertThat(entries)
                        .hasSize(100);

                // null to
                tsf.remove(DateUtils.addMinutes(baseLine, 50), null);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> entries = Arrays.asList(session.timeSeriesFor(docId, "BloodPressure")
                        .get());
                assertThat(entries)
                        .hasSize(50);
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, "BodyTemperature");

                List<TimeSeriesEntry> entries = Arrays.asList(tsf.get());
                assertThat(entries)
                        .hasSize(100);

                // null from
                tsf.remove(null, DateUtils.addMinutes(baseLine, 19));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> entries = Arrays.asList(session.timeSeriesFor(docId, "BodyTemperature")
                        .get());

                assertThat(entries)
                        .hasSize(80);
            }
        }
    }
}
