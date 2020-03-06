package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_12030Test extends RemoteTestBase {

    public static class Fox {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Fox_Search extends AbstractIndexCreationTask {
        public Fox_Search() {
            map = "from f in docs.Foxes select new { f.name }";

            index("name", FieldIndexing.SEARCH);
        }
    }

    @Test
    public void simpleFuzzy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company hr = new Company();
                hr.setName("Hibernating Rhinos");
                session.store(hr);

                Company cf = new Company();
                cf.setName("CodeForge");
                session.store(cf);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<Company> companies = session
                        .advanced()
                        .documentQuery(Company.class)
                        .whereEquals("name", "CoedForhe")
                        .fuzzy(0.5)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(companies.get(0).getName())
                        .isEqualTo("CodeForge");

                companies = session
                        .advanced()
                        .documentQuery(Company.class)
                        .whereEquals("name", "Hiberanting Rinhos")
                        .fuzzy(0.5)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
                assertThat(companies.get(0).getName())
                        .isEqualTo("Hibernating Rhinos");

                companies = session
                        .advanced()
                        .documentQuery(Company.class)
                        .whereEquals("name", "CoedForhe")
                        .fuzzy(0.99)
                        .toList();

                assertThat(companies)
                        .hasSize(0);
            }
        }
    }

    @Test
    public void simpleProximity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Fox_Search().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Fox f1 = new Fox();
                f1.setName("a quick brown fox");
                session.store(f1);

                Fox f2 = new Fox();
                f2.setName("the fox is quick");
                session.store(f2);

                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<Fox> foxes = session
                        .advanced()
                        .documentQuery(Fox.class, Fox_Search.class)
                        .search("name", "quick fox")
                        .proximity(1)
                        .toList();

                assertThat(foxes)
                        .hasSize(1);
                assertThat(foxes.get(0).getName())
                        .isEqualTo("a quick brown fox");

                foxes = session
                        .advanced()
                        .documentQuery(Fox.class, Fox_Search.class)
                        .search("name", "quick fox")
                        .proximity(2)
                        .toList();

                assertThat(foxes)
                        .hasSize(2);
                assertThat(foxes.get(0).getName())
                        .isEqualTo("a quick brown fox");
                assertThat(foxes.get(1).getName())
                        .isEqualTo("the fox is quick");
            }
        }
    }
}
