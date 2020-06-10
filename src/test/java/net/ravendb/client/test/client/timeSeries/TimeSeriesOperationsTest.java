package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRangeAggregation;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRawResult;
import net.ravendb.client.documents.queries.timeSeries.TypedTimeSeriesRawResult;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.documents.DocumentDoesNotExistException;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.TimeValue;
import org.apache.commons.collections.ArrayStack;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class TimeSeriesOperationsTest extends RemoteTestBase {

    @Test
    public void canCreateAndGetSimpleTimeSeriesUsingStoreOperations() throws Exception {
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, documentId);
                session.saveChanges();
            }

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation.AppendOperation append1 = new TimeSeriesOperation.AppendOperation();
            append1.setTag("watches/fitbit");
            append1.setTimestamp(DateUtils.addSeconds(baseLine, 1));
            append1.setValues(new double[] { 59 });

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.append(append1);

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesRangeResult timeSeriesRangeResult = store.operations()
                    .send(new GetTimeSeriesOperation(documentId, "Heartrate"));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(1);

            TimeSeriesEntry value = timeSeriesRangeResult.getEntries()[0];
            assertThat(value.getValues()[0])
                    .isEqualTo(59, Offset.offset(0.001));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));
        }
    }

    @Test
    public void canGetNonExistedRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/ayende");
                session.saveChanges();
            }

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation();
            timeSeriesOp.setName("Heartrate");
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(
                    DateUtils.addSeconds(baseLine, 1), new double[]{59}, "watches/fitbit"));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation("users/ayende", timeSeriesOp);
            store.operations().send(timeSeriesBatch);

            TimeSeriesRangeResult timeSeriesRangeResult = store.operations().send(
                    new GetTimeSeriesOperation("users/ayende", "Heartrate",
                            DateUtils.addMonths(baseLine, -2), DateUtils.addMonths(baseLine, -1)));

            assertThat(timeSeriesRangeResult.getEntries())
                    .isEmpty();
        }
    }

    @Test
    public void canStoreAndReadMultipleTimestampsUsingStoreOperations() throws Exception {
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.append(
                    new TimeSeriesOperation.AppendOperation(
                            DateUtils.addSeconds(baseLine, 1), new double[]{59}, "watches/fitbit"));
            timeSeriesOp.append(
                    new TimeSeriesOperation.AppendOperation(
                            DateUtils.addSeconds(baseLine, 2), new double[]{61}, "watches/fitbit"));
            timeSeriesOp.append(
                    new TimeSeriesOperation.AppendOperation(
                            DateUtils.addSeconds(baseLine, 5), new double[]{60}, "watches/apple-watch"));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesRangeResult timeSeriesRangeResult = store.operations()
                    .send(new GetTimeSeriesOperation(documentId, "Heartrate"));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(3);

            TimeSeriesEntry value = timeSeriesRangeResult.getEntries()[0];

            assertThat(value.getValues()[0])
                    .isEqualTo(59, Offset.offset(0.01));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));

            value = timeSeriesRangeResult.getEntries()[1];

            assertThat(value.getValues()[0])
                    .isEqualTo(61, Offset.offset(0.01));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 2));

            value = timeSeriesRangeResult.getEntries()[2];

            assertThat(value.getValues()[0])
                    .isEqualTo(60, Offset.offset(0.01));
            assertThat(value.getTag())
                    .isEqualTo("watches/apple-watch");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 5));
        }
    }

    @Test
    public void canDeleteTimestampUsingStoreOperations() throws Exception {
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 1), new double[] { 59 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 2), new double[] { 61 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 3), new double[] { 60 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 4), new double[] { 62.5 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 5), new double[] { 62 }, "watches/fitbit"));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesRangeResult timeSeriesRangeResult = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(5);

            timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.remove(new TimeSeriesOperation.RemoveOperation(DateUtils.addSeconds(baseLine, 2), DateUtils.addSeconds(baseLine, 3)));

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesRangeResult = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(3);

            TimeSeriesEntry value = timeSeriesRangeResult.getEntries()[0];
            assertThat(value.getValues()[0])
                    .isEqualTo(59);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));

            value = timeSeriesRangeResult.getEntries()[1];
            assertThat(value.getValues()[0])
                    .isEqualTo(62.5);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 4));

            value = timeSeriesRangeResult.getEntries()[2];
            assertThat(value.getValues()[0])
                    .isEqualTo(62);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 5));

            try (IDocumentSession session = store.openSession()) {
                session.delete(documentId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.saveChanges();

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 1), new double[] { 59 }, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 2), new double[] { 69 }, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 3), new double[] { 79 }, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, documentId);
                session.timeSeriesFor(documentId, "Heartrate")
                        .remove(DateUtils.addMinutes(baseLine, 2));

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> vals = Arrays.asList(session.timeSeriesFor(documentId, "Heartrate")
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
    public void canDeleteLargeRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.addSeconds(DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH), -1);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "foo/bar");
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("foo/bar", "BloodPressure");

                for (int j = 1; j < 10_000; j++) {
                    int offset = j * 10;
                    Date time = DateUtils.addSeconds(baseLine, offset);

                    tsf.append(time, new double[]{j}, "watches/apple");
                }

                session.saveChanges();
            }

            String rawQuery = "declare timeseries blood_pressure(doc)\n" +
                    "  {\n" +
                    "      from doc.BloodPressure between $start and $end\n" +
                    "      group by 1h\n" +
                    "      select min(), max(), avg(), first(), last()\n" +
                    "  }\n" +
                    "  from Users as p\n" +
                    "  select blood_pressure(p) as bloodPressure";

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawQueryTest.RawQueryResult> query = session
                        .advanced()
                        .rawQuery(TimeSeriesRawQueryTest.RawQueryResult.class, rawQuery)
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addDays(baseLine, 1));

                List<TimeSeriesRawQueryTest.RawQueryResult> result = query.toList();

                assertThat(result)
                        .hasSize(1);

                TimeSeriesRawQueryTest.RawQueryResult agg = result.get(0);

                TimeSeriesAggregationResult bloodPressure = agg.getBloodPressure();
                Long count = Arrays.stream(bloodPressure.getResults()).map(x -> x.getCount()[0])
                        .reduce(0L, Long::sum);
                assertThat(count)
                        .isEqualTo(8640);
                assertThat(count)
                        .isEqualTo(bloodPressure.getCount());
                assertThat(bloodPressure.getResults().length)
                        .isEqualTo(24);

                for (int index = 0; index < bloodPressure.getResults().length; index++) {
                    TimeSeriesRangeAggregation item = bloodPressure.getResults()[index];

                    assertThat(item.getCount()[0])
                            .isEqualTo(360);
                    assertThat(item.getAverage()[0])
                            .isEqualTo(index * 360 + 180 + 0.5);
                    assertThat(item.getMax()[0])
                            .isEqualTo((index + 1) * 360);
                    assertThat(item.getMin()[0])
                            .isEqualTo(index * 360 + 1);
                    assertThat(item.getFirst()[0])
                            .isEqualTo(index * 360 + 1);
                    assertThat(item.getLast()[0])
                            .isEqualTo((index + 1) * 360);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("foo/bar", "BloodPressure");
                tsf.remove(DateUtils.addSeconds(baseLine, 3600), DateUtils.addSeconds(baseLine, 3600 * 10)); // remove 9 hours
                session.saveChanges();
            }

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setNoCaching(true);
            try (IDocumentSession session = store.openSession(sessionOptions)) {
                IRawDocumentQuery<TimeSeriesRawQueryTest.RawQueryResult> query = session.advanced().rawQuery(TimeSeriesRawQueryTest.RawQueryResult.class, rawQuery)
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addDays(baseLine, 1));

                List<TimeSeriesRawQueryTest.RawQueryResult> result = query.toList();

                TimeSeriesRawQueryTest.RawQueryResult agg = result.get(0);

                TimeSeriesAggregationResult bloodPressure = agg.getBloodPressure();
                Long count = Arrays.stream(bloodPressure.getResults()).map(x -> x.getCount()[0])
                        .reduce(0L, Long::sum);
                assertThat(count)
                        .isEqualTo(5399);
                assertThat(count)
                        .isEqualTo(bloodPressure.getCount());
                assertThat(bloodPressure.getResults().length)
                        .isEqualTo(15);

                int index = 0;
                TimeSeriesRangeAggregation item = bloodPressure.getResults()[index];
                assertThat(item.getCount()[0])
                        .isEqualTo(359);
                assertThat(item.getAverage()[0])
                        .isEqualTo(180);
                assertThat(item.getMax()[0])
                        .isEqualTo(359);
                assertThat(item.getMin()[0])
                        .isEqualTo(1);
                assertThat(item.getFirst()[0])
                        .isEqualTo(1);
                assertThat(item.getLast()[0])
                        .isEqualTo(359);

                for (index = 1; index < bloodPressure.getResults().length; index++) {
                    item = bloodPressure.getResults()[index];
                    int realIndex = index + 9;

                    assertThat(item.getCount()[0])
                            .isEqualTo(360);
                    assertThat(item.getAverage()[0])
                            .isEqualTo(realIndex * 360 + 180 + 0.5);
                    assertThat(item.getMax()[0])
                            .isEqualTo((realIndex + 1) * 360);
                    assertThat(item.getMin()[0])
                            .isEqualTo(realIndex * 360 + 1);
                    assertThat(item.getFirst()[0])
                            .isEqualTo(realIndex * 360 + 1);
                    assertThat(item.getLast()[0])
                            .isEqualTo((realIndex + 1) * 360);
                }
            }
        }
    }

    @Test
    public void canAppendAndRemoveTimestampsInSingleBatch() throws Exception {
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 1), new double[]{59}, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 2), new double[]{61}, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 3), new double[]{61.5}, "watches/fitbit"));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesRangeResult timeSeriesRangeResult = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(3);

            timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 4), new double[] { 60 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 5), new double[] { 62.5 }, "watches/fitbit"));
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 6), new double[] { 62 }, "watches/fitbit"));

            timeSeriesOp.remove(new TimeSeriesOperation.RemoveOperation(DateUtils.addSeconds(baseline, 2), DateUtils.addSeconds(baseline, 3)));

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesRangeResult = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesRangeResult.getEntries())
                    .hasSize(4);

            TimeSeriesEntry value = timeSeriesRangeResult.getEntries()[0];
            assertThat(value.getValues()[0])
                    .isEqualTo(59);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 1));

            value = timeSeriesRangeResult.getEntries()[1];
            assertThat(value.getValues()[0])
                    .isEqualTo(60);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 4));

            value = timeSeriesRangeResult.getEntries()[2];
            assertThat(value.getValues()[0])
                    .isEqualTo(62.5);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 5));

            value = timeSeriesRangeResult.getEntries()[3];
            assertThat(value.getValues()[0])
                    .isEqualTo(62);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 6));
        }
    }

    @Test
    public void shouldThrowOnAttemptToCreateTimeSeriesOnMissingDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");
            timeSeriesOp.append(new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 1), new double[]{59}, "watches/fitbit"));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation("users/ayende", timeSeriesOp);
            assertThatThrownBy(() -> {
                store.operations().send(timeSeriesBatch);
            })
                    .isInstanceOf(DocumentDoesNotExistException.class)
                    .hasMessageContaining("Cannot operate on time series of a missing document");
        }
    }

    @Test
    public void canGetMultipleRangesInSingleRequest() throws Exception {
        String documentId = "users/ayende";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            for (int i = 0; i <= 360; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, i * 10), new double[] { 59 }, "watches/fitbit")
                );
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);
            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetMultipleTimeSeriesOperation(documentId,
                    Arrays.asList(
                            new TimeSeriesRange("Heartrate", DateUtils.addMinutes(baseline, 5), DateUtils.addMinutes(baseline, 10)),
                            new TimeSeriesRange("Heartrate", DateUtils.addMinutes(baseline, 15), DateUtils.addMinutes(baseline, 30)),
                            new TimeSeriesRange("Heartrate", DateUtils.addMinutes(baseline, 40), DateUtils.addMinutes(baseline, 60))
                    )));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate"))
                    .hasSize(3);

            TimeSeriesRangeResult range = timeSeriesDetails.getValues().get("Heartrate").get(0);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 5));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10));

            assertThat(range.getEntries())
                    .hasSize(31);
            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 5));
            assertThat(range.getEntries()[30].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10));

            range = timeSeriesDetails.getValues().get("Heartrate").get(1);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 15));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            assertThat(range.getEntries())
                    .hasSize(91);
            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 15));
            assertThat(range.getEntries()[90].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            range = timeSeriesDetails.getValues().get("Heartrate").get(2);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 40));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));

            assertThat(range.getEntries())
                    .hasSize(121);
            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 40));
            assertThat(range.getEntries()[120].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));
        }
    }

    @Test
    public void canGetMultipleTimeSeriesInSingleRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            // append

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[] { 72 }, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesOp = new TimeSeriesOperation("BloodPressure");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10),
                                new double[] { 80 }
                        )
                );
            }

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesOp = new TimeSeriesOperation("Temperature");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10),
                                new double[] { 37 + i * 0.15 }
                        )
                );
            }

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            // get ranges from multiple time series in a single request

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetMultipleTimeSeriesOperation(documentId,
                    Arrays.asList(
                            new TimeSeriesRange("Heartrate", baseline, DateUtils.addMinutes(baseline, 15)),
                            new TimeSeriesRange("Heartrate", DateUtils.addMinutes(baseline, 30), DateUtils.addMinutes(baseline, 45)),
                            new TimeSeriesRange("BloodPressure", baseline, DateUtils.addMinutes(baseline, 30)),
                            new TimeSeriesRange("BloodPressure", DateUtils.addMinutes(baseline, 60), DateUtils.addMinutes(baseline, 90)),
                            new TimeSeriesRange("Temperature", baseline, DateUtils.addDays(baseline, 1))
                    )));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo("users/ayende");
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(3);

            assertThat(timeSeriesDetails.getValues().get("Heartrate"))
                    .hasSize(2);

            TimeSeriesRangeResult range = timeSeriesDetails.getValues().get("Heartrate").get(0);
            assertThat(range.getFrom())
                    .isEqualTo(baseline);
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 15));

            assertThat(range.getEntries())
                    .hasSize(2);

            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries()[1].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10));

            assertThat(range.getTotalResults())
                    .isNull();

            range = timeSeriesDetails.getValues().get("Heartrate").get(1);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 45));

            assertThat(range.getEntries())
                    .hasSize(2);
            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));
            assertThat(range.getEntries()[1].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 40));

            assertThat(range.getTotalResults())
                    .isNull();

            assertThat(timeSeriesDetails.getValues().get("BloodPressure"))
                    .hasSize(2);

            range = timeSeriesDetails.getValues().get("BloodPressure").get(0);

            assertThat(range.getFrom())
                    .isEqualTo(baseline);
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            assertThat(range.getEntries())
                    .hasSize(4);

            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries()[3].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            assertThat(range.getTotalResults())
                    .isNull();

            range = timeSeriesDetails.getValues().get("BloodPressure").get(1);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 90));

            assertThat(range.getEntries())
                    .hasSize(4);

            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));
            assertThat(range.getEntries()[3].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 90));

            assertThat(range.getTotalResults())
                    .isNull();

            assertThat(timeSeriesDetails.getValues().get("Temperature"))
                    .hasSize(1);

            range = timeSeriesDetails.getValues().get("Temperature").get(0);

            assertThat(range.getFrom())
                    .isEqualTo(baseline);
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addDays(baseline, 1));

            assertThat(range.getEntries())
                    .hasSize(11);

            assertThat(range.getEntries()[0].getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries()[10].getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 100));

            assertThat(range.getTotalResults())
                    .isEqualTo(11); // full range
        }
    }

    @Test
    public void shouldThrowOnNullOrEmptyRanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[] { 72 }, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            assertThatThrownBy(() -> {
                store.operations().send(new GetTimeSeriesOperation("users/ayende", null));
            })
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> {
                store.operations().send(new GetMultipleTimeSeriesOperation("users/ayende", new ArrayList<>()));
            })
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void getMultipleTimeSeriesShouldThrowOnMissingNameFromRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[]{72}, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            assertThatThrownBy(() -> {
                store.operations().send(new GetMultipleTimeSeriesOperation("users/ayende",
                        Collections.singletonList(
                                new TimeSeriesRange(null, baseline, null)
                        )));
            })
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Name cannot be null or empty");
        }
    }

    @Test
    public void getTimeSeriesShouldThrowOnMissingName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate");

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.append(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[]{72}, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            assertThatThrownBy(() -> {
                store.operations().send(new GetTimeSeriesOperation("users/ayende", "", baseline, DateUtils.addYears(baseline, 10)));
            })
                    .hasMessageContaining("Timeseries cannot be null or empty");
        }
    }

    @Test
    public void getTimeSeriesStatistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";
            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, documentId);

                ISessionDocumentTimeSeries ts = session.timeSeriesFor(documentId, "heartrate");
                for (int i = 0; i <= 10; i++) {
                    ts.append(DateUtils.addMinutes(baseline, i * 10), 72, "watches/fitbit");
                }

                ts = session.timeSeriesFor(documentId, "pressure");
                for (int i = 10; i <= 20; i++) {
                    ts.append(DateUtils.addMinutes(baseline, i * 10), 72, "watches/fitbit");
                }

                session.saveChanges();
            }

            TimeSeriesStatistics op = store.operations().send(new GetTimeSeriesStatisticsOperation(documentId));

            assertThat(op.getDocumentId())
                    .isEqualTo(documentId);
            assertThat(op.getTimeSeries())
                    .hasSize(2);

            TimeSeriesItemDetail ts1 = op.getTimeSeries().get(0);
            TimeSeriesItemDetail ts2 = op.getTimeSeries().get(1);

            assertThat(ts1.getName())
                    .isEqualTo("heartrate");
            assertThat(ts2.getName())
                    .isEqualTo("pressure");

            assertThat(ts1.getNumberOfEntries())
                    .isEqualTo(11);
            assertThat(ts2.getNumberOfEntries())
                    .isEqualTo(11);

            assertThat(ts1.getStartDate())
                    .isEqualTo(baseline);
            assertThat(ts1.getEndDate())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10 * 10));

            assertThat(ts2.getStartDate())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10 * 10));
            assertThat(ts2.getEndDate())
                    .isEqualTo(DateUtils.addMinutes(baseline, 20 * 10));
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

            TimeSeriesRangeResult get = store.operations().send(new GetTimeSeriesOperation(docId, "HeartRate"));
            assertThat(get.getEntries())
                    .hasSize(100);

            // null From, To

            TimeSeriesOperation deleteOp = new TimeSeriesOperation();
            deleteOp.setName("Heartrate");
            deleteOp.remove(new TimeSeriesOperation.RemoveOperation());

            store.operations().send(new TimeSeriesBatchOperation(docId, deleteOp));

            get = store.operations().send(new GetTimeSeriesOperation(docId, "HeartRate"));

            assertThat(get)
                    .isNull();

            get = store.operations().send(new GetTimeSeriesOperation(docId, "BloodPressure"));
            assertThat(get.getEntries())
                    .hasSize(100);

            // null to

            deleteOp = new TimeSeriesOperation();
            deleteOp.setName("BloodPressure");
            deleteOp.remove(new TimeSeriesOperation.RemoveOperation(DateUtils.addMinutes(baseLine, 50), null));

            store.operations().send(new TimeSeriesBatchOperation(docId, deleteOp));

            get = store.operations().send(new GetTimeSeriesOperation(docId, "BloodPressure"));
            assertThat(get.getEntries())
                    .hasSize(50);

            get = store.operations().send(new GetTimeSeriesOperation(docId, "BodyTemperature"));
            assertThat(get.getEntries())
                    .hasSize(100);

            // null From
            deleteOp = new TimeSeriesOperation();
            deleteOp.setName("BodyTemperature");
            deleteOp.remove(new TimeSeriesOperation.RemoveOperation(null, DateUtils.addMinutes(baseLine, 19)));

            store.operations().send(new TimeSeriesBatchOperation(docId, deleteOp));

            get = store.operations().send(new GetTimeSeriesOperation(docId, "BodyTemperature"));
            assertThat(get.getEntries())
                    .hasSize(80);
        }
    }

    @Test
    public void getOperationShouldIncludeValuesFromRollUpsInResult() throws Exception {
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

            // please notice we don't modify server time here!

            Date now = new Date();
            Date baseline = DateUtils.addDays(now, -12);

            int total = (int) Duration.ofDays(12).get(ChronoUnit.SECONDS) / 60;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");

                for (int i = 0; i <= total; i++) {
                    session.timeSeriesFor("users/karmel", "Heartrate")
                            .append(DateUtils.addMinutes(baseline, i), i, "watches/fitbit");
                }

                session.saveChanges();
            }

            Thread.sleep(1500); // wait for rollup

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(TimeSeriesRawResult.class, "declare timeseries out()\n" +
                        "{\n" +
                        "    from Heartrate\n" +
                        "    between $start and $end\n" +
                        "}\n" +
                        "from Users as u\n" +
                        "select out()")
                        .addParameter("start", DateUtils.addDays(baseline, -1))
                        .addParameter("end", DateUtils.addDays(now, 1));

                TimeSeriesRawResult result = query.single();

                assertThat(result.getResults().length)
                        .isPositive();

                for (TimeSeriesEntry res : result.getResults()) {

                    if (res.isRollup()) {
                        assertThat(res.getValues().length)
                                .isPositive();
                        assertThat(res.getValues()[0])
                                .isPositive();
                    } else {
                        assertThat(res.getValues())
                                .hasSize(1);
                    }
                }
            }
        }
    }
}
