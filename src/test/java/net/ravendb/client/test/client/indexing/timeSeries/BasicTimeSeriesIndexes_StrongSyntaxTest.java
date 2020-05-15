package net.ravendb.client.test.client.indexing.timeSeries;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.timeSeries.AbstractMultiMapTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.AbstractTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.TimeSeriesIndexDefinition;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicTimeSeriesIndexes_StrongSyntaxTest extends RemoteTestBase {

    @Test
    public void basicMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date now1 = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.timeSeriesFor(company, "HeartRate")
                        .append(now1, 7, "tag");

                session.saveChanges();
            }

            MyTsIndex timeSeriesIndex = new MyTsIndex();
            TimeSeriesIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            assertThat(indexDefinition)
                    .isNotNull();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<MyMultiMapTsIndex.Result> results = session.query(MyMultiMapTsIndex.Result.class, MyTsIndex.class)
                        .toList();

                assertThat(results)
                        .hasSize(1);

                MyMultiMapTsIndex.Result result = results.get(0);

                assertThat(result.getDate())
                        .isEqualTo(now1);
                assertThat(result.getUser())
                        .isNotNull();
                assertThat(result.getHeartBeat())
                        .isPositive();
            }
        }
    }

    @Test
    public void basicMultiMapIndex() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            MyMultiMapTsIndex timeSeriesIndex = new MyMultiMapTsIndex();
            timeSeriesIndex.execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company);

                session.timeSeriesFor(company, "HeartRate")
                        .append(now, 2.5, "tag1");
                session.timeSeriesFor(company, "HeartRate2")
                        .append(now, 3.5, "tag2");

                User user = new User();
                session.store(user);
                session.timeSeriesFor(user, "HeartRate")
                        .append(now, 4.5, "tag3");

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<MyMultiMapTsIndex.Result> results = session.query(MyMultiMapTsIndex.Result.class, MyMultiMapTsIndex.class)
                        .toList();

                assertThat(results)
                        .hasSize(3);

                MyMultiMapTsIndex.Result result = results.get(0);

                assertThat(result.getDate())
                        .isEqualTo(now);
                assertThat(result.getUser())
                        .isNotNull();
                assertThat(result.getHeartBeat())
                        .isPositive();
            }
        }
    }

    public static class MyTsIndex extends AbstractTimeSeriesIndexCreationTask {
        public MyTsIndex() {
            map = "from ts in timeSeries.Companies.HeartRate " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   date = entry.Timestamp.Date, " +
                    "   user = ts.DocumentId " +
                    "}";
        }
    }

    public static class MyMultiMapTsIndex extends AbstractMultiMapTimeSeriesIndexCreationTask {

        public static class Result {
            private double heartBeat;
            private Date date;
            private String user;

            public double getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(double heartBeat) {
                this.heartBeat = heartBeat;
            }

            public Date getDate() {
                return date;
            }

            public void setDate(Date date) {
                this.date = date;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }
        }

        public MyMultiMapTsIndex() {
            addMap( "from ts in timeSeries.Companies.HeartRate " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   date = entry.Timestamp.Date, " +
                    "   user = ts.DocumentId " +
                    "}");

            addMap( "from ts in timeSeries.Companies.HeartRate2 " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   date = entry.Timestamp.Date, " +
                    "   user = ts.DocumentId " +
                    "}");

            addMap( "from ts in timeSeries.Users.HeartRate " +
                    "from entry in ts.Entries " +
                    "select new { " +
                    "   heartBeat = entry.Values[0], " +
                    "   date = entry.Timestamp.Date, " +
                    "   user = ts.DocumentId " +
                    "}");
        }
    }
}
