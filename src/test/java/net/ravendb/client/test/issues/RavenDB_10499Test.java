package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.primitives.CleanCloseable;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_10499Test extends RemoteTestBase {

    @Test
    public void canLoadAggressivelyCachingAfterDbInitialized() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "myDocuments/123";

            try (IDocumentSession session = store.openSession()) {
                Document loaded = session.load(Document.class, id);
                if (loaded == null) {
                    Document doc = new Document();
                    doc.setId(id);
                    doc.setName("document");
                    session.store(doc);
                    session.saveChanges();
                }
            }

            // now that program has started it's safe to use aggressive caching as database is in valid state

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable cache = store.aggressivelyCacheFor(Duration.ofMinutes(1))) {
                    Document loaded = session.load(Document.class, id);
                    assertThat(loaded)
                            .isNotNull();
                }
            }
        }
    }

    public static class Document {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
