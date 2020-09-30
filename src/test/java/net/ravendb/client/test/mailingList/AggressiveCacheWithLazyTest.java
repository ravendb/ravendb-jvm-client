package net.ravendb.client.test.mailingList;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AggressiveCacheWithLazyTest extends RemoteTestBase {

    @Test
    public void aggresiveCacheWithLazyTest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            try (IDocumentSession session = store.openSession()) {
                Doc doc = new Doc();
                doc.setId("doc-1");
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> docLazy = session.advanced().lazily().load(Doc.class, "doc-1");
                    Doc doc = docLazy.getValue();
                }
            }

            long requests = requestExecutor.numberOfServerRequests.get();

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> cachedDocLazy = session.advanced().lazily().load(Doc.class, "doc-1");
                    Doc cachedDoc = cachedDocLazy.getValue();
                }
            }

            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isEqualTo(requests);
        }
    }

    @Test
    public void aggresiveCacheWithLazyTestAsync_Partly() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();
            try (IDocumentSession session = store.openSession()) {
                Doc doc = new Doc();
                doc.setId("doc-1");
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> docLazy = session.advanced().lazily().load(Doc.class, "doc-1");
                    Doc doc = docLazy.getValue();
                }
            }

            long requests = requestExecutor.numberOfServerRequests.get();

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMillis(5))) {
                    Lazy<Doc> cachedDocLazy = session.advanced().lazily().load(Doc.class, "doc-1");
                    session.advanced().lazily().load(Doc.class, "doc-2"); // not used
                    Doc cachedDoc = cachedDocLazy.getValue();
                }
            }

            // should force a call here
            assertThat(requestExecutor.numberOfServerRequests.get())
                    .isBetween(requests + 1, requests + 2);
        }
    }

    public static class Doc {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
