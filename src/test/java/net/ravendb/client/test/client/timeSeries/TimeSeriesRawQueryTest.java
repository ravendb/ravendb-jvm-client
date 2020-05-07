package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRangeAggregation;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRawResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesRawQueryTest extends RemoteTestBase {

    public static class PeopleIndex extends AbstractIndexCreationTask {
        public PeopleIndex() {
            map = "from p in docs.People select new { p.age }";
        }

        @Override
        public String getIndexName() {
            return "People";
        }
    }

    @Test
    public void canQueryTimeSeriesAggregation_DeclareSyntax_WithOtherFields() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i <= 3; i++) {
                    String id = "people/" + i;

                    Person person = new Person();
                    person.setName("Oren");
                    person.setAge(i * 30);

                    session.store(person, id);

                    ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, "Heartrate");

                    tsf.append(DateUtils.addMinutes(baseLine, 61), 59, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 62), 79, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 63), 69, "watches/fitbit");

                    session.saveChanges();
                }
            }

            new PeopleIndex().execute(store);

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<RawQueryResult> query = session.advanced().rawQuery(RawQueryResult.class, "declare timeseries out(p)\n" +
                        "{\n" +
                        "    from p.HeartRate between $start and $end\n" +
                        "    group by 1h\n" +
                        "    select min(), max()\n" +
                        "}\n" +
                        "from index 'People' as p\n" +
                        "where p.age > 49\n" +
                        "select out(p) as heartRate, p.name")
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addDays(baseLine, 1));

                List<RawQueryResult> result = query.toList();

                assertThat(result)
                        .hasSize(2);

                for (int i = 0; i < 2; i++) {
                    RawQueryResult agg = result.get(i);

                    assertThat(agg.getName())
                            .isEqualTo("Oren");

                    TimeSeriesAggregationResult heartrate = agg.getHeartRate();

                    assertThat(heartrate.getCount())
                            .isEqualTo(3);

                    assertThat(heartrate.getResults())
                            .hasSize(1);

                    TimeSeriesRangeAggregation val = heartrate.getResults()[0];

                    assertThat(val.getMin()[0])
                            .isEqualTo(59);
                    assertThat(val.getMax()[0])
                            .isEqualTo(79);

                    assertThat(val.getFrom())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 60));
                    assertThat(val.getTo())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 120));
                }
            }
        }
    }

    @Test
    public void canQueryTimeSeriesAggregation_DeclareSyntax_MultipleSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            Date baseLine2 = DateUtils.addDays(baseLine, -1);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i <= 3; i++) {
                    String id = "people/" + i;

                    Person person = new Person();
                    person.setName("Oren");
                    person.setAge(i * 30);

                    session.store(person, id);

                    ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, "Heartrate");

                    tsf.append(DateUtils.addMinutes(baseLine, 61), 59, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 62), 79, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 63), 69, "watches/fitbit");

                    tsf = session.timeSeriesFor(id, "BloodPressure");

                    tsf.append(DateUtils.addMinutes(baseLine2, 61), 159, "watches/apple");
                    tsf.append(DateUtils.addMinutes(baseLine2, 62), 179, "watches/apple");
                    tsf.append(DateUtils.addMinutes(baseLine2, 63), 168, "watches/apple");

                    session.saveChanges();
                }
            }

            new PeopleIndex().execute(store);

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<RawQueryResult> query = session.advanced().rawQuery(RawQueryResult.class, "declare timeseries heart_rate(doc)\n" +
                        "{\n" +
                        "    from doc.HeartRate between $start and $end\n" +
                        "    group by 1h\n" +
                        "    select min(), max()\n" +
                        "}\n" +
                        "declare timeseries blood_pressure(doc)\n" +
                        "{\n" +
                        "    from doc.BloodPressure between $start2 and $end2\n" +
                        "    group by 1h\n" +
                        "    select min(), max(), avg()\n" +
                        "}\n" +
                        "from index 'People' as p\n" +
                        "where p.age > 49\n" +
                        "select heart_rate(p) as heartRate, blood_pressure(p) as bloodPressure")
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addDays(baseLine, 1))
                        .addParameter("start2", baseLine2)
                        .addParameter("end2", DateUtils.addDays(baseLine2, 1));

                List<RawQueryResult> result = query.toList();

                assertThat(result)
                        .hasSize(2);

                for (int i = 0; i < 2; i++) {
                    RawQueryResult agg = result.get(i);

                    TimeSeriesAggregationResult heartRate = agg.getHeartRate();
                    assertThat(heartRate.getCount())
                            .isEqualTo(3);
                    assertThat(heartRate.getResults())
                            .hasSize(1);

                    TimeSeriesRangeAggregation val = heartRate.getResults()[0];

                    assertThat(val.getMin()[0])
                            .isEqualTo(59);
                    assertThat(val.getMax()[0])
                            .isEqualTo(79);

                    assertThat(val.getFrom())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 60));
                    assertThat(val.getTo())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 120));

                    TimeSeriesAggregationResult bloodPressure = agg.getBloodPressure();

                    assertThat(bloodPressure.getCount())
                            .isEqualTo(3);

                    assertThat(bloodPressure.getResults())
                            .hasSize(1);

                    val = bloodPressure.getResults()[0];

                    assertThat(val.getMin()[0])
                            .isEqualTo(159);
                    assertThat(val.getMax()[0])
                            .isEqualTo(179);

                    double expectedAvg =(159 +  168 + 179) / 3.0;

                    assertThat(val.getAverage()[0])
                            .isEqualTo(expectedAvg);

                    assertThat(val.getFrom())
                            .isEqualTo(DateUtils.addMinutes(baseLine2, 60));
                    assertThat(val.getTo())
                            .isEqualTo(DateUtils.addMinutes(baseLine2, 120));

                }
            }
        }
    }

    @Test
    public void canQueryTimeSeriesAggregation_NoSelectOrGroupBy_MultipleValues() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i <= 3; i++) {
                    String id = "people/" + i;

                    Person person = new Person();
                    person.setName("Oren");
                    person.setAge(i * 30);

                    session.store(person, id);

                    ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, "Heartrate");

                    tsf.append(DateUtils.addMinutes(baseLine, 61), new double[] { 59, 159 }, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 62), new double[] { 79, 179 }, "watches/fitbit");
                    tsf.append(DateUtils.addMinutes(baseLine, 63), 69, "watches/apple");

                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1), new double[] { 159, 259 }, "watches/fitbit");
                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1), new double[] { 179 }, "watches/apple");
                    tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1), new double[] { 169, 269 }, "watches/fitbit");

                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(
                        TimeSeriesRawResult.class, "declare timeseries out(x)\n" +
                        "{\n" +
                        "    from x.HeartRate between $start and $end\n" +
                        "}\n" +
                        "from People as doc\n" +
                        "where doc.age > 49\n" +
                        "select out(doc)")
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addMonths(baseLine, 2));

                List<TimeSeriesRawResult> result = query.toList();

                assertThat(result)
                        .hasSize(2);

                for (int i = 0; i < 2; i++) {
                    TimeSeriesRawResult agg = result.get(i);

                    assertThat(agg.getResults())
                            .hasSize(6);

                    TimeSeriesEntry val = agg.getResults()[0];

                    assertThat(val.getValues())
                            .hasSize(2);
                    assertThat(val.getValues()[0])
                            .isEqualTo(59);
                    assertThat(val.getValues()[1])
                            .isEqualTo(159);

                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 61));

                    val = agg.getResults()[1];

                    assertThat(val.getValues())
                            .hasSize(2);
                    assertThat(val.getValues()[0])
                            .isEqualTo(79);
                    assertThat(val.getValues()[1])
                            .isEqualTo(179);

                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 62));

                    val = agg.getResults()[2];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValues()[0])
                            .isEqualTo(69);

                    assertThat(val.getTag())
                            .isEqualTo("watches/apple");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMinutes(baseLine, 63));

                    val = agg.getResults()[3];

                    assertThat(val.getValues())
                            .hasSize(2);
                    assertThat(val.getValues()[0])
                            .isEqualTo(159);
                    assertThat(val.getValues()[1])
                            .isEqualTo(259);

                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1));

                    val = agg.getResults()[4];

                    assertThat(val.getValues())
                            .hasSize(1);
                    assertThat(val.getValues()[0])
                            .isEqualTo(179);

                    assertThat(val.getTag())
                            .isEqualTo("watches/apple");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1));

                    val = agg.getResults()[5];

                    assertThat(val.getValues())
                            .hasSize(2);
                    assertThat(val.getValues()[0])
                            .isEqualTo(169);
                    assertThat(val.getValues()[1])
                            .isEqualTo(269);

                    assertThat(val.getTag())
                            .isEqualTo("watches/fitbit");
                    assertThat(val.getTimestamp())
                            .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1));
                }
            }
        }
    }

    public static class RawQueryResult {
        private TimeSeriesAggregationResult heartRate;
        private TimeSeriesAggregationResult bloodPressure;
        private String name;

        public TimeSeriesAggregationResult getHeartRate() {
            return heartRate;
        }

        public void setHeartRate(TimeSeriesAggregationResult heartRate) {
            this.heartRate = heartRate;
        }

        public TimeSeriesAggregationResult getBloodPressure() {
            return bloodPressure;
        }

        public void setBloodPressure(TimeSeriesAggregationResult bloodPressure) {
            this.bloodPressure = bloodPressure;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Person {
        private String name;
        private int age;
        private String worksAt;
        private String event;
        private AdditionalData additionalData;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getWorksAt() {
            return worksAt;
        }

        public void setWorksAt(String worksAt) {
            this.worksAt = worksAt;
        }

        public String getEvent() {
            return event;
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public AdditionalData getAdditionalData() {
            return additionalData;
        }

        public void setAdditionalData(AdditionalData additionalData) {
            this.additionalData = additionalData;
        }
    }

    public static class AdditionalData {
        private NestedClass nestedClass;

        public NestedClass getNestedClass() {
            return nestedClass;
        }

        public void setNestedClass(NestedClass nestedClass) {
            this.nestedClass = nestedClass;
        }
    }

    public static class NestedClass {
        private Event event;
        private double accuracy;

        public Event getEvent() {
            return event;
        }

        public void setEvent(Event event) {
            this.event = event;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }
    }

    public static class Event {
        private Date start;
        private Date end;
        private String description;

        public Date getStart() {
            return start;
        }

        public void setStart(Date start) {
            this.start = start;
        }

        public Date getEnd() {
            return end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
