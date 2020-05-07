package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_10520Test extends RemoteTestBase {

    @Test
    public void queryCanReturnResultAsArray() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new RavenDB_9745Test.Companies_ByName().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("Micro");

                Company company2 = new Company();
                company2.setName("Microsoft");

                session.store(company1);
                session.store(company2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Company[] companies = session
                        .advanced()
                        .documentQuery(Company.class)
                        .search("name", "Micro*")
                        .toArray();

                assertThat(companies)
                        .hasSize(2);
            }

            try (IDocumentSession session = store.openSession()) {
                Company[] companies = session
                        .query(Company.class)
                        .toArray();

                assertThat(companies)
                        .hasSize(2);
            }

            try (IDocumentSession session = store.openSession()) {
                CompanyName[] companies = session
                        .query(Company.class)
                        .selectFields(CompanyName.class)
                        .toArray();

                assertThat(companies)
                        .hasSize(2);
            }
        }
    }

    public static class CompanyName {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
