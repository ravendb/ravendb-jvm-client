package net.ravendb.client.test.client.indexing.timeSeries;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractJavaScriptIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.AbstractJavaScriptTimeSeriesIndexCreationTask;
import net.ravendb.client.documents.indexes.timeSeries.TimeSeriesIndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetTermsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Employee;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicTimeSeriesIndexes_JavaScript extends RemoteTestBase {

    public static class MyTsIndex_AllTimeSeries extends AbstractJavaScriptTimeSeriesIndexCreationTask {
        public MyTsIndex_AllTimeSeries() {
            setMaps(Collections.singleton("timeSeries.map('Companies', function (ts) {\n" +
                    " return ts.Entries.map(entry => ({\n" +
                    "        heartBeat: entry.Values[0],\n" +
                    "        date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        user: ts.documentId\n" +
                    "    }));\n" +
                    " })"));
        }
    }

    public static class MyTsIndex_Load extends AbstractJavaScriptTimeSeriesIndexCreationTask {
        public MyTsIndex_Load() {
            setMaps(Collections.singleton("timeSeries.map('Companies', 'HeartRate', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        heartBeat: entry.Value,\n" +
                    "        date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        user: ts.DocumentId,\n" +
                    "        employee: load(entry.Tag, 'Employees').firstName\n" +
                    "    }));\n" +
                    "})"));
        }
    }

    public static class AverageHeartRateDaily_ByDateAndCity extends AbstractJavaScriptTimeSeriesIndexCreationTask {
        public static class Result {
            private double heartBeat;
            private Date date;
            private String city;
            private long count;

            public Date getDate() {
                return date;
            }

            public void setDate(Date date) {
                this.date = date;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public long getCount() {
                return count;
            }

            public void setCount(long count) {
                this.count = count;
            }

            public double getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(double heartBeat) {
                this.heartBeat = heartBeat;
            }
        }

        public AverageHeartRateDaily_ByDateAndCity() {
            setMaps(Collections.singleton("timeSeries.map('Users', 'heartRate', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        heartBeat: entry.Value,\n" +
                    "        date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        city: load(entry.Tag, 'Addresses').city,\n" +
                    "        count: 1\n" +
                    "    }));\n" +
                    "})"));

            setReduce("groupBy(r => ({ date: r.date, city: r.city }))\n" +
                    " .aggregate(g => ({\n" +
                    "     heartBeat: g.values.reduce((total, val) => val.heartBeat + total, 0) / g.values.reduce((total, val) => val.count + total, 0),\n" +
                    "     date: g.key.date,\n" +
                    "     city: g.key.city,\n" +
                    "     count: g.values.reduce((total, val) => val.count + total, 0)\n" +
                    " }))");
        }
    }

    public static class MyMultiMapTsIndex extends AbstractJavaScriptTimeSeriesIndexCreationTask {
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
            Set<String> maps = new HashSet<>();

            maps.add("timeSeries.map('Companies', 'HeartRate', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        HeartBeat: entry.Values[0],\n" +
                    "        Date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        User: ts.DocumentId\n" +
                    "    }));\n" +
                    "})");

            maps.add("timeSeries.map('Companies', 'HeartRate2', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        HeartBeat: entry.Values[0],\n" +
                    "        Date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        User: ts.DocumentId\n" +
                    "    }));\n" +
                    "})");

            maps.add("timeSeries.map('Users', 'HeartRate', function (ts) {\n" +
                    "return ts.Entries.map(entry => ({\n" +
                    "        HeartBeat: entry.Values[0],\n" +
                    "        Date: new Date(entry.Timestamp.getFullYear(), entry.Timestamp.getMonth(), entry.Timestamp.getDate()),\n" +
                    "        User: ts.DocumentId\n" +
                    "    }));\n" +
                    "})");

            setMaps(maps);
        }
    }

    public static class Companies_ByTimeSeriesNames extends AbstractJavaScriptIndexCreationTask {
        public Companies_ByTimeSeriesNames() {
            setMaps(Collections.singleton("map('Companies', function (company) {\n" +
                    "return ({\n" +
                    "    names: timeSeriesNamesFor(company)\n" +
                    "})\n" +
                    "})"));
        }
    }

    @Test
    public void basicMapIndexWithLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date now1 = new Date();
            Date now2 = DateUtils.addSeconds(now1, 1);

            try (IDocumentSession session = store.openSession()) {
                Employee employee = new Employee();
                employee.setFirstName("John");

                session.store(employee, "employees/1");

                Company company = new Company();
                session.store(company, "companies/1");

                session.timeSeriesFor(company, "HeartRate")
                        .append(now1, 7.0, employee.getId());

                Company company2 = new Company();
                session.store(company2, "companies/11");

                session.timeSeriesFor(company2, "HeartRate")
                        .append(now1, 11.0, employee.getId());

                session.saveChanges();
            }

            MyTsIndex_Load timeSeriesIndex = new MyTsIndex_Load();
            String indexName = timeSeriesIndex.getIndexName();
            TimeSeriesIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation(indexName, "employee", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("john");

            try (IDocumentSession session = store.openSession()) {
                Employee employee = session.load(Employee.class, "employees/1");
                employee.setFirstName("Bob");
                session.saveChanges();
            }

            waitForIndexing(store);

            terms = store.maintenance().send(new GetTermsOperation(indexName, "employee", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("bob");

            try (IDocumentSession session = store.openSession()) {
                session.delete("employees/1");
                session.saveChanges();
            }

            waitForIndexing(store);

            terms = store.maintenance().send(new GetTermsOperation(indexName, "employee", null));
            assertThat(terms)
                    .hasSize(0);
        }
    }

    @Test
    public void basicMapReduceIndexWithLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date today = RavenTestHelper.utcToday();

            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("NY");

                session.store(address, "addresses/1");

                User user = new User();
                user.setAddressId(address.getId());

                session.store(user, "users/1");

                for (int i = 0; i < 10; i++) {
                    session.timeSeriesFor(user, "heartRate")
                            .append(DateUtils.addHours(today, i), 180 + i, address.getId());
                }

                session.saveChanges();
            }

            AverageHeartRateDaily_ByDateAndCity timeSeriesIndex = new AverageHeartRateDaily_ByDateAndCity();
            String indexName = timeSeriesIndex.getIndexName();
            TimeSeriesIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation(indexName, "heartBeat", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("184.5");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "date", null));
            assertThat(terms)
                    .hasSize(1);

            terms = store.maintenance().send(new GetTermsOperation(indexName, "city", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("ny");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "count", null));
            assertThat(terms)
                    .hasSize(1);
            assertThat(terms[0])
                    .isEqualTo("10");

            try (IDocumentSession session = store.openSession()) {
                Address address = session.load(Address.class, "addresses/1");
                address.setCity("LA");
                session.saveChanges();
            }

            waitForIndexing(store);

            terms = store.maintenance().send(new GetTermsOperation(indexName, "city", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("la");
        }
    }

    @Test
    public void canMapAllTimeSeriesFromCollection() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date now1 = new Date();
            Date now2 = DateUtils.addSeconds(now1, 1);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.timeSeriesFor(company, "heartRate")
                        .append(now1, 7.0, "tag1");
                session.timeSeriesFor(company, "likes")
                        .append(now1, 3.0, "tag2");

                session.saveChanges();
            }

            new MyTsIndex_AllTimeSeries().execute(store);

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation("MyTsIndex/AllTimeSeries", "heartBeat", null));
            assertThat(terms)
                    .hasSize(2)
                    .contains("7")
                    .contains("3");
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

                session.timeSeriesFor(company, "heartRate")
                        .append(now, 2.5, "tag1");
                session.timeSeriesFor(company, "heartRate2")
                        .append(now, 3.5, "tag2");

                User user = new User();
                session.store(user);
                session.timeSeriesFor(user, "heartRate")
                        .append(now, 4.5, "tag3");

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<MyMultiMapTsIndex.Result> results = session.query(MyMultiMapTsIndex.Result.class, MyMultiMapTsIndex.class)
                        .toList();

                assertThat(results)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void timeSeriesNamesFor() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            Companies_ByTimeSeriesNames index = new Companies_ByTimeSeriesNames();
            index.execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");

                session.saveChanges();
            }

            waitForIndexing(store);

            RavenTestHelper.assertNoIndexErrors(store);

            String[] terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "names", null));
            assertThat(terms)
                    .hasSize(0);

            terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "names_IsArray", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("true");

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");
                session.timeSeriesFor(company, "heartRate")
                        .append(now, 2.5, "tag1");
                session.timeSeriesFor(company, "heartRate2")
                        .append(now, 3.5, "tag2");

                session.saveChanges();
            }

            waitForIndexing(store);

            RavenTestHelper.assertNoIndexErrors(store);

            terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "names", null));
            assertThat(terms)
                    .hasSize(2)
                    .contains("heartrate")
                    .contains("heartrate2");

            terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "names_IsArray", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("true");
        }
    }
}
