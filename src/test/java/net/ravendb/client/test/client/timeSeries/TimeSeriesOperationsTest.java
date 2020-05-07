package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRangeAggregation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.documents.DocumentDoesNotExistException;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", Collections.singletonList(append1), null);

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations()
                    .send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().size())
                    .isOne();

            TimeSeriesEntry value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(0);
            assertThat(value.getValues()[0])
                    .isEqualTo(59, Offset.offset(0.001));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));
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
            timeSeriesOp.setAppends(Arrays.asList(
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 1), new double[]{59}, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 2), new double[]{61}, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 5), new double[]{60}, "watches/apple-watch")
            ));

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations()
                    .send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries())
                    .hasSize(3);

            TimeSeriesEntry value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(0);

            assertThat(value.getValues()[0])
                    .isEqualTo(59, Offset.offset(0.01));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(1);

            assertThat(value.getValues()[0])
                    .isEqualTo(61, Offset.offset(0.01));
            assertThat(value.getTag())
                    .isEqualTo("watches/fitbit");
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 2));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(2);

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

            List<TimeSeriesOperation.AppendOperation> appends = Arrays.asList(
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 1), new double[] { 59 }, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 2), new double[] { 61 }, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 3), new double[] { 60 }, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 4), new double[] { 62.5 }, "watches/fitbit"),
                    new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseLine, 5), new double[] { 62 }, "watches/fitbit")
            );

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", appends, null);

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries())
                    .hasSize(5);

            timeSeriesOp = new TimeSeriesOperation("Heartrate", null, Collections.singletonList(
                    new TimeSeriesOperation.RemoveOperation(DateUtils.addSeconds(baseLine, 2), DateUtils.addSeconds(baseLine, 3))));

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries())
                    .hasSize(3);

            TimeSeriesEntry value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(0);
            assertThat(value.getValues()[0])
                    .isEqualTo(59);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 1));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(1);
            assertThat(value.getValues()[0])
                    .isEqualTo(62.5);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseLine, 4));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(2);
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
                List<TimeSeriesEntry> vals = session.timeSeriesFor(documentId, "Heartrate")
                        .get(null, null);

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

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate",
                    Arrays.asList(
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 1), new double[]{59}, "watches/fitbit"),
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 2), new double[]{61}, "watches/fitbit"),
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 3), new double[]{61.5}, "watches/fitbit")
                    ), null);

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries())
                    .hasSize(3);

            timeSeriesOp = new TimeSeriesOperation("Heartrate",
                    Arrays.asList(
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 4), new double[] { 60 }, "watches/fitbit"),
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 5), new double[] { 62.5 }, "watches/fitbit"),
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 6), new double[] { 62 }, "watches/fitbit")
                    ),
                    Arrays.asList(
                            new TimeSeriesOperation.RemoveOperation(DateUtils.addSeconds(baseline, 2), DateUtils.addSeconds(baseline, 3))
                    ));

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId, "Heartrate", null, null));

            assertThat(timeSeriesDetails.getId())
                    .isEqualTo(documentId);
            assertThat(timeSeriesDetails.getValues())
                    .hasSize(1);
            assertThat(timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries())
                    .hasSize(4);

            TimeSeriesEntry value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(0);
            assertThat(value.getValues()[0])
                    .isEqualTo(59);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 1));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(1);
            assertThat(value.getValues()[0])
                    .isEqualTo(60);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 4));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(2);
            assertThat(value.getValues()[0])
                    .isEqualTo(62.5);
            assertThat(value.getTimestamp())
                    .isEqualTo(DateUtils.addSeconds(baseline, 5));

            value = timeSeriesDetails.getValues().get("Heartrate").get(0).getEntries().get(3);
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

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate",
                    Arrays.asList(
                            new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, 1), new double[]{59}, "watches/fitbit")
                    ), null);

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

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", new ArrayList<>(), null);

            for (int i = 0; i <= 360; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(DateUtils.addSeconds(baseline, i * 10), new double[] { 59 }, "watches/fitbit")
                );
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);
            store.operations().send(timeSeriesBatch);

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId,
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
            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 5));
            assertThat(range.getEntries().get(30).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 10));

            range = timeSeriesDetails.getValues().get("Heartrate").get(1);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 15));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            assertThat(range.getEntries())
                    .hasSize(91);
            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 15));
            assertThat(range.getEntries().get(90).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));

            range = timeSeriesDetails.getValues().get("Heartrate").get(2);

            assertThat(range.getFrom())
                    .isEqualTo(DateUtils.addMinutes(baseline, 40));
            assertThat(range.getTo())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));

            assertThat(range.getEntries())
                    .hasSize(121);
            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 40));
            assertThat(range.getEntries().get(120).getTimestamp())
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

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", new ArrayList<>(), null);

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[] { 72 }, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesOp = new TimeSeriesOperation("BloodPressure", new ArrayList<>(), null);

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10),
                                new double[] { 80 }
                        )
                );
            }

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            timeSeriesOp = new TimeSeriesOperation("Temperature", new ArrayList<>(), null);

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10),
                                new double[] { 37 + i * 0.15 }
                        )
                );
            }

            timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            // get ranges from multiple time series in a single request

            TimeSeriesDetails timeSeriesDetails = store.operations().send(new GetTimeSeriesOperation(documentId,
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

            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries().get(1).getTimestamp())
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
            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 30));
            assertThat(range.getEntries().get(1).getTimestamp())
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

            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries().get(3).getTimestamp())
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

            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 60));
            assertThat(range.getEntries().get(3).getTimestamp())
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

            assertThat(range.getEntries().get(0).getTimestamp())
                    .isEqualTo(baseline);
            assertThat(range.getEntries().get(10).getTimestamp())
                    .isEqualTo(DateUtils.addMinutes(baseline, 100));

            assertThat(range.getTotalResults())
                    .isEqualTo(11); // full range
        }
    }

    @Test
    public void shouldThrowOnNullRanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", new ArrayList<>(), null);

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[] { 72 }, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            assertThatThrownBy(() -> {
                store.operations().send(new GetTimeSeriesOperation("users/ayende", null));
            })
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void shouldThrowOnMissingName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            Date baseline = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            TimeSeriesOperation timeSeriesOp = new TimeSeriesOperation("Heartrate", new ArrayList<>(), null);

            for (int i = 0; i <= 10; i++) {
                timeSeriesOp.getAppends().add(
                        new TimeSeriesOperation.AppendOperation(
                                DateUtils.addMinutes(baseline, i * 10), new double[]{72}, "watches/fitbit"));
            }

            TimeSeriesBatchOperation timeSeriesBatch = new TimeSeriesBatchOperation(documentId, timeSeriesOp);

            store.operations().send(timeSeriesBatch);

            assertThatThrownBy(() -> {
                store.operations().send(new GetTimeSeriesOperation("users/ayende",
                        Arrays.asList(
                                new TimeSeriesRange(null, baseline, null)
                        )));
            })
                    .isInstanceOf(RavenException.class);
        }
    }
}
