package net.ravendb.client.test.mailingList;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadAllStartingWith extends RemoteTestBase {

    public static class Abc {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Xyz {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    @Test
    public void loadAllStartingWith() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Abc doc1 = new Abc();
            doc1.setId("abc/1");

            Xyz doc2 = new Xyz();
            doc2.setId("xyz/1");

            try (IDocumentSession session = store.openSession()) {
                session.store(doc1);
                session.store(doc2);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<Map<String, Abc>> testClasses =
                        session.advanced().lazily().loadStartingWith(Abc.class, "abc/");

                List<Xyz> test2Classes = session.query(Xyz.class).waitForNonStaleResults()
                        .lazily().getValue();

                assertThat(testClasses.getValue())
                        .hasSize(1);

                assertThat(test2Classes)
                        .hasSize(1);

                assertThat(testClasses.getValue().get("abc/1").getId())
                        .isEqualTo("abc/1");

                assertThat(test2Classes.get(0).getId())
                        .isEqualTo("xyz/1");
            }
        }
    }
}
