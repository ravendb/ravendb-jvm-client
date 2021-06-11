package net.ravendb.client.test.client.indexing.counters;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractJavaScriptIndexCreationTask;
import net.ravendb.client.documents.indexes.counters.AbstractJavaScriptCountersIndexCreationTask;
import net.ravendb.client.documents.indexes.counters.CountersIndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetTermsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicCountersIndexes_JavaScript extends RemoteTestBase {

    public static class MyCounterIndex extends AbstractJavaScriptCountersIndexCreationTask {
        public MyCounterIndex() {
            setMaps(Collections.singleton("counters.map('Companies', 'HeartRate', function (counter) {\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    name: counter.Name,\n" +
                    "    user: counter.DocumentId\n" +
                    "};\n" +
                    "})"));
        }
    }

    public static class AverageHeartRate_WithLoad extends AbstractJavaScriptCountersIndexCreationTask {
        public static class Result {
            private double heartBeat;
            private String city;
            private long count;

            public double getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(double heartBeat) {
                this.heartBeat = heartBeat;
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
        }

        public AverageHeartRate_WithLoad() {
            setMaps(Collections.singleton("counters.map('Users', 'heartRate', function (counter) {\n" +
                    "var user = load(counter.DocumentId, 'Users');\n" +
                    "var address = load(user.addressId, 'Addresses');\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    count: 1,\n" +
                    "    city: address.city\n" +
                    "};\n" +
                    "})"));

            setReduce("groupBy(r => ({ city: r.city }))\n" +
                    " .aggregate(g => ({\n" +
                    "     heartBeat: g.values.reduce((total, val) => val.heartBeat + total, 0) / g.values.reduce((total, val) => val.count + total, 0),\n" +
                    "     city: g.key.city,\n" +
                    "     count: g.values.reduce((total, val) => val.count + total, 0)\n" +
                    " }))");
        }
    }

    public static class MyCounterIndex_AllCounters extends AbstractJavaScriptCountersIndexCreationTask {
        public MyCounterIndex_AllCounters() {
            setMaps(Collections.singleton("counters.map('Companies', function (counter) {\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    name: counter.Name,\n" +
                    "    user: counter.DocumentId\n" +
                    "};\n" +
                    "})"));
        }
    }

    public static class MyMultiMapCounterIndex extends AbstractJavaScriptCountersIndexCreationTask {
        public static class Result {
            private double heartBeat;
            private String name;
            private String user;

            public double getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(double heartBeat) {
                this.heartBeat = heartBeat;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }
        }

        public MyMultiMapCounterIndex() {
            Set<String> maps = new HashSet<>();
            setMaps(maps);

            maps.add("counters.map('Companies', 'heartRate', function (counter) {\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    name: counter.Name,\n" +
                    "    user: counter.DocumentId\n" +
                    "};\n" +
                    "})");

            maps.add("counters.map('Companies', 'heartRate2', function (counter) {\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    name: counter.Name,\n" +
                    "    user: counter.DocumentId\n" +
                    "};\n" +
                    "})");

            maps.add("counters.map('Users', 'heartRate', function (counter) {\n" +
                    "return {\n" +
                    "    heartBeat: counter.Value,\n" +
                    "    name: counter.Name,\n" +
                    "    user: counter.DocumentId\n" +
                    "};\n" +
                    "})");
        }
    }

    public static class Companies_ByCounterNames extends AbstractJavaScriptIndexCreationTask {
        public Companies_ByCounterNames() {
            setMaps(Collections.singleton("map('Companies', function (company) {\n" +
                    "return ({\n" +
                    "    names: counterNamesFor(company)\n" +
                    "})\n" +
                    "})"));
        }
    }

    @Test
    public void basicMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.countersFor(company).increment("heartRate", 7);

                session.saveChanges();
            }

            MyCounterIndex timeSeriesIndex = new MyCounterIndex();
            CountersIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "heartBeat", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("7");

            terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "user", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("companies/1");

            terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "name", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("heartrate");

            try (IDocumentSession session = store.openSession()) {
                Company company1 = session.load(Company.class, "companies/1");
                session.countersFor(company1)
                        .increment("heartRate", 3);

                Company company2 = new Company();
                session.store(company2, "companies/2");
                session.countersFor(company2)
                        .increment("heartRate", 4);

                Company company3 = new Company();
                session.store(company3, "companies/3");
                session.countersFor(company3)
                        .increment("heartRate", 6);

                Company company999 = new Company();
                session.store(company999, "companies/999");
                session.countersFor(company999)
                        .increment("heartRate_Different", 999);

                session.saveChanges();
            }

            waitForIndexing(store);

            terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "heartBeat", null));
            assertThat(terms)
                    .hasSize(3)
                    .contains("10")
                    .contains("4")
                    .contains("6");

            terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "user", null));
            assertThat(terms)
                    .hasSize(3)
                    .contains("companies/1")
                    .contains("companies/2")
                    .contains("companies/3");

            terms = store.maintenance().send(new GetTermsOperation("MyCounterIndex", "name", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("heartrate");

            // skipped rest of the test
        }
    }

    @Test
    public void basicMapReduceIndexWithLoad() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 10; i++) {
                    Address address = new Address();
                    address.setCity("NY");
                    session.store(address, "addresses/" + i);

                    User user = new User();
                    user.setAddressId(address.getId());
                    session.store(user, "users/" + i);

                    session.countersFor(user)
                            .increment("heartRate", 180 + i);
                }

                session.saveChanges();
            }

            AverageHeartRate_WithLoad timeSeriesIndex = new AverageHeartRate_WithLoad();
            String indexName = timeSeriesIndex.getIndexName();
            CountersIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);


            String[] terms = store.maintenance().send(new GetTermsOperation(indexName, "heartBeat", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("184.5");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "count", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("10");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "city", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("ny");
        }
    }

    @Test
    public void canMapAllCountersFromCollection() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.countersFor(company)
                        .increment("heartRate", 7);
                session.countersFor(company)
                        .increment("likes", 3);

                session.saveChanges();
            }

            MyCounterIndex_AllCounters timeSeriesIndex = new MyCounterIndex_AllCounters();
            String indexName = timeSeriesIndex.getIndexName();
            CountersIndexDefinition indexDefinition = timeSeriesIndex.createIndexDefinition();

            timeSeriesIndex.execute(store);

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation(indexName, "heartBeat", null));
            assertThat(terms)
                    .hasSize(2)
                    .contains("7")
                    .contains("3");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "user", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("companies/1");

            terms = store.maintenance().send(new GetTermsOperation(indexName, "name", null));
            assertThat(terms)
                    .hasSize(2)
                    .contains("heartrate")
                    .contains("likes");
        }
    }

    @Test
    public void basicMultiMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            MyMultiMapCounterIndex timeSeriesIndex = new MyMultiMapCounterIndex();
            timeSeriesIndex.execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company);

                session.countersFor(company)
                        .increment("heartRate", 3);
                session.countersFor(company)
                        .increment("heartRate2", 5);

                User user = new User();
                session.store(user);
                session.countersFor(user)
                        .increment("heartRate", 2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<MyMultiMapCounterIndex.Result> results = session.query(MyMultiMapCounterIndex.Result.class, MyMultiMapCounterIndex.class)
                        .toList();

                assertThat(results)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void counterNamesFor() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Companies_ByCounterNames index = new Companies_ByCounterNames();
            index.execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");

                session.saveChanges();
            }

            waitForIndexing(store);

            String[] terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "name", null));
            assertThat(terms)
                    .hasSize(0);

            terms = store.maintenance().send(new GetTermsOperation(index.getIndexName(), "names_IsArray", null));
            assertThat(terms)
                    .hasSize(1)
                    .contains("true");

            try (IDocumentSession session = store.openSession()) {
                Company company = session.load(Company.class, "companies/1");

                session.countersFor(company)
                        .increment("heartRate", 3);
                session.countersFor(company)
                        .increment("heartRate2", 7);

                session.saveChanges();
            }

            waitForIndexing(store);

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
