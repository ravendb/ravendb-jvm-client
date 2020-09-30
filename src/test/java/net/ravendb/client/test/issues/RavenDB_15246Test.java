package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.GetMultipleTimeSeriesOperation;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesDetails;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15246Test extends RemoteTestBase {

    @Test
    public void testClientCacheWithPageSize() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1-A");
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/1-A", "Heartrate");
                for (int i = 0; i <= 20; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                ISessionDocumentTimeSeries ts = session.timeSeriesFor(user, "Heartrate");
                TimeSeriesEntry[] res = ts.get(null, null, 0, 0);
                assertThat(res)
                        .isEmpty();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                res = ts.get(0, 10);
                assertThat(res)
                        .hasSize(10);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                res = ts.get(0, 7);
                assertThat(res)
                        .hasSize(7);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                res = ts.get(0, 20);
                assertThat(res)
                        .hasSize(20);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                res = ts.get(0, 25);
                assertThat(res)
                        .hasSize(21);

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void testRanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String id = "users/1-A";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), id);
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, "raven");
                for (int i = 0; i <= 10; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                for (int i = 12; i <= 13; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                for (int i = 16; i <= 20; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                session.saveChanges();
            }

            List<TimeSeriesRange> rangesList = new ArrayList<>();
            TimeSeriesRange timeSeriesRange = new TimeSeriesRange();
            timeSeriesRange.setName("raven");
            timeSeriesRange.setFrom(DateUtils.addMinutes(baseLine, 1));
            timeSeriesRange.setTo(DateUtils.addMilliseconds(baseLine, 7));
            rangesList.add(timeSeriesRange);

            RequestExecutor re = store.getRequestExecutor();
            GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand tsCommand
                    = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            TimeSeriesDetails res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            rangesList = Collections.singletonList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 11)));

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            rangesList = Collections.singletonList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 17)));

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            rangesList = Collections.singletonList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 14), DateUtils.addMinutes(baseLine, 15)));

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            rangesList = Collections.singletonList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 23), DateUtils.addMinutes(baseLine, 25)));

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            rangesList = Collections.singletonList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 20), DateUtils.addMinutes(baseLine, 26)));

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, Integer.MAX_VALUE);
            re.execute(tsCommand);
            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);
        }
    }

    @Test
    public void testClientCacheWithStart() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1-A");
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/1-A", "Heartrate");
                for (int i = 0; i < 20; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1-A");
                ISessionDocumentTimeSeries ts = session.timeSeriesFor(user, "Heartrate");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);

                TimeSeriesEntry[] res = ts.get(20, Integer.MAX_VALUE);
                assertThat(res)
                        .isNull();
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                res = ts.get( 10, Integer.MAX_VALUE);
                assertThat(res)
                        .hasSize(10);
                assertThat(res[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                res = ts.get(0, Integer.MAX_VALUE);
                assertThat(res)
                        .hasSize(20);
                assertThat(res[0].getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(res[10].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                res = ts.get(10, Integer.MAX_VALUE);
                assertThat(res)
                        .hasSize(10);
                assertThat(res[0].getTimestamp())
                        .isEqualTo(DateUtils.addMinutes(baseLine, 10));
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                res = ts.get(20, Integer.MAX_VALUE);
                assertThat(res)
                        .isEmpty();
            }
        }
    }

    @Test
    public void getResultsWithRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            String id = "users/1-A";
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), id);
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, "raven");
                for (int i = 0; i < 8; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { 64 }, "watches/apple");
                }

                session.saveChanges();

                List<TimeSeriesRange> rangesList = Arrays.asList(
                        new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 0), DateUtils.addMinutes(baseLine, 3)),
                        new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 4), DateUtils.addMinutes(baseLine, 7)),
                        new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 11))
                );

                RequestExecutor re = store.getRequestExecutor();

                GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand tsCommand
                        = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 10);
                re.execute(tsCommand);

                TimeSeriesDetails res = tsCommand.getResult();

                assertThat(res.getValues())
                        .hasSize(1);
                assertThat(res.getValues().get("raven"))
                        .hasSize(3);

                assertThat(res.getValues().get("raven").get(0).getEntries())
                        .hasSize(4);
                assertThat(res.getValues().get("raven").get(1).getEntries())
                        .hasSize(4);
                assertThat(res.getValues().get("raven").get(2).getEntries())
                        .hasSize(0);

                tsf = session.timeSeriesFor(id, "raven");
                for (int i = 8; i < 11; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { 1000 }, "watches/apple");
                }

                session.saveChanges();

                tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 10);

                re.execute(tsCommand);

                res = tsCommand.getResult();

                assertThat(res.getValues())
                        .hasSize(1);
                assertThat(res.getValues().get("raven"))
                        .hasSize(3);

                assertThat(res.getValues().get("raven").get(0).getEntries())
                        .hasSize(4);
                assertThat(res.getValues().get("raven").get(1).getEntries())
                        .hasSize(4);
                assertThat(res.getValues().get("raven").get(2).getEntries())
                        .hasSize(2);
            }
        }
    }

    @Test
    public void resultsWithRangeAndPageSize() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String tag = "raven";
            String id = "users/1";
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), id);
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, tag);
                for (int i = 0; i <= 15; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                session.saveChanges();
            }

            List<TimeSeriesRange> rangesList = Arrays.asList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 0), DateUtils.addMinutes(baseLine, 3)),
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 4), DateUtils.addMinutes(baseLine, 7)),
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 11))
            );

            RequestExecutor re = store.getRequestExecutor();

            GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 0);
            re.execute(tsCommand);

            TimeSeriesDetails res = tsCommand.getResult();
            assertThat(res.getValues())
                    .isEmpty();

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 30);
            re.execute(tsCommand);

            res =tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(3);

            assertThat(res.getValues().get("raven").get(0).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(1).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(2).getEntries())
                    .hasSize(4);

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 6);
            re.execute(tsCommand);

            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(2);

            assertThat(res.getValues().get("raven").get(0).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(1).getEntries())
                    .hasSize(2);
        }
    }

    @Test
    public void resultsWithRangeAndStart() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String tag = "raven";
            String id = "users/1";
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), id);
                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(id, tag);
                for (int i = 0; i <= 15; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), new double[] { i }, "watches/apple");
                }
                session.saveChanges();
            }

            List<TimeSeriesRange> rangesList = Arrays.asList(
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 0), DateUtils.addMinutes(baseLine, 3)),
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 4), DateUtils.addMinutes(baseLine, 7)),
                    new TimeSeriesRange("raven", DateUtils.addMinutes(baseLine, 8), DateUtils.addMinutes(baseLine, 11))
            );

            RequestExecutor re = store.getRequestExecutor();

            GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand tsCommand
                    = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 0, 20);

            re.execute(tsCommand);

            TimeSeriesDetails res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(3);

            assertThat(res.getValues().get("raven").get(0).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(1).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(2).getEntries())
                    .hasSize(4);

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 3, 20);
            re.execute(tsCommand);

            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(3);

            assertThat(res.getValues().get("raven").get(0).getEntries())
                    .hasSize(1);
            assertThat(res.getValues().get("raven").get(1).getEntries())
                    .hasSize(4);
            assertThat(res.getValues().get("raven").get(2).getEntries())
                    .hasSize(4);

            tsCommand = new GetMultipleTimeSeriesOperation.GetMultipleTimeSeriesCommand(id, rangesList, 9, 20);
            re.execute(tsCommand);

            res = tsCommand.getResult();

            assertThat(res.getValues())
                    .hasSize(1);
            assertThat(res.getValues().get("raven"))
                    .hasSize(1);

            assertThat(res.getValues().get("raven").get(0).getEntries())
                    .hasSize(3);
        }
    }
}
