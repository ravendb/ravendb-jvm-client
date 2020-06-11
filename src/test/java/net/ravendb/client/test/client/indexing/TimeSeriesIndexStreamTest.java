package net.ravendb.client.test.client.indexing;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.indexes.timeSeries.AbstractTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.infrastructure.entities.Company;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeSeriesIndexStreamTest extends RemoteTestBase {

    @Test
    public void basicMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date now1 = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
            Date now2 = DateUtils.addSeconds(now1, 1);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                ISessionDocumentTimeSeries ts = session.timeSeriesFor(company, "HeartRate");

                for (int i = 0; i < 10; i++) {
                    ts.append(DateUtils.addMinutes(now1, i), i , "tag");
                }

                session.saveChanges();
            }

            MyTsIndex timeSeriesIndex = new MyTsIndex();
            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                int i = 0;

                try (CloseableIterator<StreamResult<MyTsIndex.Result>> stream = session.advanced().stream(session.query(MyTsIndex.Result.class, MyTsIndex.class))) {
                    while (stream.hasNext()) {
                        MyTsIndex.Result results = stream.next().getDocument();
                        assertThat(results.getTimestamp())
                                .isEqualTo(DateUtils.addMinutes(now1, i));
                        assertThat(results.getHeartBeat())
                                .isEqualTo(i);
                        assertThat(results.getUser())
                                .isEqualTo("companies/1");
                        i++;
                    }
                }

                assertThat(i)
                        .isEqualTo(10);
            }
        }
    }

    public static class MyTsIndex extends AbstractTimeSeriesIndexCreationTask {

        public static class Result {
            private double heartBeat;
            private Date timestamp;
            private String user;

            public double getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(double heartBeat) {
                this.heartBeat = heartBeat;
            }

            public Date getTimestamp() {
                return timestamp;
            }

            public void setTimestamp(Date timestamp) {
                this.timestamp = timestamp;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }
        }

        public MyTsIndex() {
            map = "from ts in timeSeries.Companies.HeartRate " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   timestamp = entry.Timestamp, " +
                    "   user = ts.DocumentId " +
                    "}";
        }
    }
}
