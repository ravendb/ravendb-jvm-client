package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimeSeriesBulkInsertTest extends RemoteTestBase {

    @Test
    public void canCreateSimpleTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry val = session.timeSeriesFor(documentId, "Heartrate")
                        .get()[0];

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
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 60, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] val = session.timeSeriesFor(documentId, "Heartrate")
                        .get();
                assertThat(val)
                        .hasSize(2);
            }
        }
    }

    @Test
    public void canDeleteTimestamp() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert
                             = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 69, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 79, "watches/fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");

                session.store(user, documentId);

                session.timeSeriesFor(documentId, "Heartrate")
                        .delete(DateUtils.addMinutes(baseLine, 2));
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get());

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

            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 70, "watches/apple");
                }
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
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), new double[] { 70, 120, 80 }, "watches/apple");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 69, "watches/fitbit");
                }
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
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), new double[]{ 70, 120, 80}, "watches/apple");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 69, "watches/fitbit");
                }
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
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), new double[]{ 59 }, "watches/fitbit");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 62, "watches/apple-watch");
                }
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
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.saveChanges();
            }

            int offset = 0;

            for (int i = 0; i < 10; i++) {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                        for (int j = 0; j < 1000; j++) {
                            timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset++), new double[] { offset }, "watches/fitbit");
                        }
                    }

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
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.saveChanges();
            }

            final int retries = 1000;

            int offset = 0;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    for (int j = 0; j < retries; j++) {

                        timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset), new double[] { offset }, "watches/fitbit");

                        offset += 5;
                    }
                }
            }

            offset = 1;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    for (int j = 0; j < retries; j++) {
                        timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset), new double[] { offset }, "watches/fitbit");
                        offset += 5;
                    }
                }
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
    public void canRequestNonExistingTimeSeriesRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(baseLine, 58, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 10), 60, "watches/fitbit");
                }
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
        String documentId1 = "users/karmel";
        String documentId2 = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                bulkInsert.store(user, documentId1);
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId1, "Nasdaq2")) {
                    timeSeriesBulkInsert.append(new Date(), 7547.31, "web");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId1, "Heartrate2")) {
                    timeSeriesBulkInsert.append(new Date(), 7547.31, "web");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                bulkInsert.store(user, documentId2);
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "Nasdaq")) {
                    timeSeriesBulkInsert.append(new Date(), 7547.31, "web");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "Heartrate")) {
                    timeSeriesBulkInsert.append(new Date(), 58, "fitbit");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, documentId2);
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
                User user = session.load(User.class, documentId1);
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(2);

                // should be sorted
                assertThat(tsNames.get(0))
                        .isEqualTo("Heartrate2");
                assertThat(tsNames.get(1))
                        .isEqualTo("Nasdaq2");
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 58, "fitbit");
                }
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
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            int offset = 0;

            for (int i = 0; i < 100; i++) {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                        for (int j = 0; j < 1000; j++) {
                            timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset++), offset, "watches/fitbit");
                        }
                    }
                }
            }

            offset = 0;

            for (int i = 0; i < 100; i++) {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Pulse")) {
                        for (int j = 0; j < 1000; j++) {
                            timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset++), offset, "watches/fitbit");
                        }
                    }
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

            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 59, "watches/fitbit");
                }

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate2")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/apple");
                }
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete(documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry[] vals = session.timeSeriesFor(documentId, "Heartrate")
                        .get(null, null);
                assertThat(vals)
                        .isNull();

                vals = session.timeSeriesFor(documentId, "Heartrate2")
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
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    for (int i = 0; i < 100; i++) {
                        timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, i), 100 + i, "watches/fitbit");
                    }
                }
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
    public void canStoreAndReadMultipleTimeseriesForDifferentDocuments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId1 = "users/ayende";
            String documentId2 = "users/grisha";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("Oren");
                bulkInsert.store(user1, documentId1);
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId1, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                }

                User user2 = new User();
                user2.setName("Grisha");
                bulkInsert.store(user2, documentId2);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");
                }
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId1, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");
                }

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 2), 61, "watches/fitbit");
                }

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId1, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 62, "watches/apple-watch");
                }

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId2, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 3), 62, "watches/apple-watch");
                }
            }

            Consumer<List<TimeSeriesEntry>> validateValues = (vals) -> {
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
            };

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(documentId1, "Heartrate")
                        .get());

                validateValues.accept(vals);

                vals = Arrays.asList(session.timeSeriesFor(documentId2, "Heartrate")
                        .get());

                validateValues.accept(vals);
            }
        }
    }

    @Test
    public void canAppendALotOfTimeSeries() throws Exception {
        int numberOfTimeSeries = 10 * 1024;

        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId = "users/ayende";

            int offset = 0;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    for (int j = 0; j < numberOfTimeSeries; j++) {
                        timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, offset++), offset, "watches/fitbit");
                    }
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
                        .get());

                assertThat(vals)
                        .hasSize(numberOfTimeSeries);

                for (int i = 0; i < numberOfTimeSeries; i++) {
                    assertThat(vals.get(i).getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, i));
                    assertThat(vals.get(i).getValues()[0])
                            .isEqualTo(1 + i);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/ayende");
                List<String> tsNames = session.advanced().getTimeSeriesFor(user);
                assertThat(tsNames)
                        .hasSize(1);

                assertThat(tsNames.get(0))
                        .isEqualTo("Heartrate");
            }
        }
    }

    @Test
    public void errorHandling() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String documentId = "users/ayende";

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Oren");
                bulkInsert.store(user, documentId);

                try (BulkInsertOperation.TimeSeriesBulkInsert timeSeriesBulkInsert = bulkInsert.timeSeriesFor(documentId, "Heartrate")) {
                    timeSeriesBulkInsert.append(DateUtils.addMinutes(baseLine, 1), 59, "watches/fitbit");

                    String errorMessage = "There is an already running time series operation, did you forget to close it?";

                    assertThatThrownBy(() -> {
                        User user1 = new User();
                        user1.setName("Oren");
                        bulkInsert.store(user1);
                    })
                            .hasMessageContaining(errorMessage)
                            .isInstanceOf(IllegalStateException.class);

                    assertThatThrownBy(() -> bulkInsert.countersFor("test").increment("1", 1))
                            .hasMessageContaining(errorMessage)
                            .isInstanceOf(IllegalStateException.class);

                    assertThatThrownBy(() -> bulkInsert.timeSeriesFor(documentId, "Pulse"))
                            .hasMessageContaining(errorMessage)
                            .isInstanceOf(IllegalStateException.class);

                    assertThatThrownBy(() -> bulkInsert.timeSeriesFor(documentId, "Heartrate"))
                            .hasMessageContaining(errorMessage)
                            .isInstanceOf(IllegalStateException.class);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                TimeSeriesEntry val = session.timeSeriesFor(documentId, "Heartrate")
                        .get()[0];

                assertThat(val.getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(val.getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(val.getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 1));
            }
        }
    }
}
