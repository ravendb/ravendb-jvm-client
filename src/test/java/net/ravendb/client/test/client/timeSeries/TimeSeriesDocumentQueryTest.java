package net.ravendb.client.test.client.timeSeries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesAggregationResult;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRangeAggregation;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRawResult;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesDocumentQueryTest extends RemoteTestBase {

    @Test
    public void canQueryTimeSeriesUsingDocumentQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                user.setAge(35);

                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 61), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 62), 79, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 63), 69, "watches/fitbit");

                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1), new double[] { 159 }, "watches/apple");
                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1), new double[] { 179 }, "watches/apple");
                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1), new double[] { 169 }, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                String tsQueryText = "from Heartrate between $start and $end\n" +
                        "where Tag = 'watches/fitbit'\n" +
                        "group by '1 month'\n" +
                        "select min(), max(), avg()";

                IDocumentQuery<TimeSeriesAggregationResult> query = session.advanced().documentQuery(User.class)
                        .whereGreaterThan("age", 21)
                        .selectTimeSeries(TimeSeriesAggregationResult.class, b -> b.raw(tsQueryText))
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addMonths(baseLine, 3));

                List<TimeSeriesAggregationResult> result = query.toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0).getCount())
                        .isEqualTo(3);

                TimeSeriesRangeAggregation[] agg = result.get(0).getResults();
                assertThat(agg)
                        .hasSize(2);

                assertThat(agg[0].getMax()[0])
                        .isEqualTo(69);
                assertThat(agg[0].getMin()[0])
                        .isEqualTo(59);
                assertThat(agg[0].getAverage()[0])
                        .isEqualTo(64);

                assertThat(agg[1].getMax()[0])
                        .isEqualTo(169);
                assertThat(agg[1].getMin()[0])
                        .isEqualTo(169);
                assertThat(agg[1].getAverage()[0])
                        .isEqualTo(169);

            }
        }
    }

    @Test
    public void canQueryTimeSeriesRawValuesUsingDocumentQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                user.setAge(35);

                session.store(user, "users/ayende");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/ayende", "Heartrate");
                tsf.append(DateUtils.addMinutes(baseLine, 61), 59, "watches/fitbit");
                tsf.append(DateUtils.addMinutes(baseLine, 62), 79, "watches/apple");
                tsf.append(DateUtils.addMinutes(baseLine, 63), 69, "watches/fitbit");

                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 61), 1), new double[] { 159 }, "watches/apple");
                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 62), 1), new double[] { 179 }, "watches/apple");
                tsf.append(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1), new double[] { 169 }, "watches/fitbit");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                String tsQueryText = "from Heartrate between $start and $end\n" +
                        "where Tag = 'watches/fitbit'";

                IDocumentQuery<TimeSeriesRawResult> query = session.advanced().documentQuery(User.class)
                        .whereGreaterThan("age", 21)
                        .selectTimeSeries(TimeSeriesRawResult.class, b -> b.raw(tsQueryText))
                        .addParameter("start", baseLine)
                        .addParameter("end", DateUtils.addMonths(baseLine, 3));

                List<TimeSeriesRawResult> result = query.toList();

                assertThat(result)
                        .hasSize(1);
                assertThat(result.get(0).getCount())
                        .isEqualTo(3);

                TimeSeriesEntry[] values = result.get(0).getResults();

                assertThat(values)
                        .hasSize(3);

                assertThat(values[0].getValues())
                        .isEqualTo(new double[] { 59 });
                assertThat(values[0].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(values[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 61));

                assertThat(values[1].getValues())
                        .isEqualTo(new double[] { 69 });
                assertThat(values[1].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(values[1].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 63));

                assertThat(values[2].getValues())
                        .isEqualTo(new double[] { 169 });
                assertThat(values[2].getTag())
                        .isEqualTo("watches/fitbit");
                assertThat(values[2].getTimestamp())
                        .isEqualTo(DateUtils.addMonths(DateUtils.addMinutes(baseLine, 63), 1));
            }
        }
    }
}
