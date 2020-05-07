package net.ravendb.client.test.client.indexing.counters;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.counters.AbstractCountersIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BasicCountersIndexes_StrongSyntaxTest extends RemoteTestBase {

    @Test
    public void basicMapIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                session.store(company, "companies/1");
                session.countersFor(company)
                        .increment("HeartRate", 7);
                session.saveChanges();
            }

            MyCounterIndex myCounterIndex = new MyCounterIndex();
            myCounterIndex.execute(store);

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<MyCounterIndex.Result> results = session.query(MyCounterIndex.Result.class, MyCounterIndex.class)
                        .toList();

                assertThat(results)
                        .hasSize(1);

                MyCounterIndex.Result result = results.get(0);

                assertThat(result.getHeartBeat())
                        .isEqualTo("7");
                assertThat(result.getUser())
                        .isEqualTo("companies/1");
                assertThat(result.getName())
                        .isEqualTo("HeartRate");
            }
        }
    }

    public static class MyCounterIndex extends AbstractCountersIndexCreationTask {
        public static class Result {
            private String heartBeat;
            private String name;
            private String user;

            public String getHeartBeat() {
                return heartBeat;
            }

            public void setHeartBeat(String heartBeat) {
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

        public MyCounterIndex() {
            map =  "counters.Companies.HeartRate.Select(counter => new {\n" +
                    "    heartBeat = counter.Value,\n" +
                    "    name = counter.Name,\n" +
                    "    user = counter.DocumentId\n" +
                    "})";
        }
    }
}
