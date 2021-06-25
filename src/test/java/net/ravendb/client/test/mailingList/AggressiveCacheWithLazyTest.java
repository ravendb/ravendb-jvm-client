package net.ravendb.client.test.mailingList;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class AggressiveCacheWithLazyTest extends RemoteTestBase {

    @Test
    public void aggresiveCacheWithLazyTest() throws Exception {
        String docId = "doc-1";

        try (IDocumentStore store = getDocumentStore()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            try (IDocumentSession session = store.openSession()) {
                Doc doc = new Doc();
                doc.setId(docId);
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> docLazy = session.advanced().lazily().load(Doc.class, docId);
                    Doc doc = docLazy.getValue();

                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(1);
                }
            }

            long requests = requestExecutor.numberOfServerRequests.get();

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> cachedDocLazy = session.advanced().lazily().load(Doc.class, "doc-1");
                    Doc cachedDoc = cachedDocLazy.getValue();

                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(0);
                }
            }
        }
    }

    @Test
    public void aggresiveCacheWithLazyTestAsync_Partly() throws Exception {
        String loadedDocId = "doc-1";
        String unloadedDocId = "doc-2";

        try (IDocumentStore store = getDocumentStore()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();
            try (IDocumentSession session = store.openSession()) {
                Doc doc = new Doc();
                doc.setId(loadedDocId);
                session.store(doc);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> docLazy = session.advanced().lazily().load(Doc.class, loadedDocId);
                    Doc doc = docLazy.getValue();
                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(1);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5))) {
                    Lazy<Doc> docLazy = session.advanced().lazily().load(Doc.class, loadedDocId);
                    Doc doc = docLazy.getValue();
                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(0);
                }
            }

            long requests = requestExecutor.numberOfServerRequests.get();

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable context = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMillis(5))) {
                    Lazy<Doc> cachedDocLazy = session.advanced().lazily().load(Doc.class, loadedDocId);
                    session.advanced().lazily().load(Doc.class, unloadedDocId); // not used
                    Doc cachedDoc = cachedDocLazy.getValue();

                    assertThat(session.advanced().getNumberOfRequests())
                            .isEqualTo(1);
                }
            }
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
