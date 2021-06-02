package net.ravendb.client.bugs.caching;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.changes.DocumentChange;
import net.ravendb.client.documents.changes.IChangesObservable;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.changes.Observers;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.MultiGetCommand;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.operations.MultiGetOperation;
import net.ravendb.client.http.AggressiveCacheMode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

public class UseCachingInLazyTest extends RemoteTestBase {

    private static BiConsumer<IDocumentSession, String> loadFunc = (s, id) -> s.load(Doc.class, id);
    private static BiConsumer<IDocumentSession, String> lazilyLoadFunc = (s, id) -> s.advanced().lazily().load(Doc.class, id).getValue();

    @Test
    public void load_WhenDoNotTrackChanges_ShouldNotCreateExtraRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            int numberOfRequest = aggressiveCacheOnLazilyLoadTest(store, loadFunc, AggressiveCacheMode.DO_NOT_TRACK_CHANGES, true);
            assertThat(numberOfRequest)
                    .isEqualTo(0);
        }
    }

    @Test
    public void lazilyLoad_WhenDoNotTrackChanges_ShouldNotCreateExtraRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            int numberOfRequest = aggressiveCacheOnLazilyLoadTest(store, lazilyLoadFunc, AggressiveCacheMode.DO_NOT_TRACK_CHANGES, true);
            assertThat(numberOfRequest)
                    .isEqualTo(0);
        }
    }

    @Test
    public void lazilyLoad_WhenTrackChangesAndChange_ShouldCreateExtraRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            int numberOfRequest = aggressiveCacheOnLazilyLoadTest(store, lazilyLoadFunc, AggressiveCacheMode.TRACK_CHANGES, true);
            assertThat(numberOfRequest)
                    .isEqualTo(1);
        }
    }

    @Test
    public void lazilyLoad_WhenTrackChangesAndDoesntChange_ShouldNotCreateExtraRequest() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            int numberOfRequest = aggressiveCacheOnLazilyLoadTest(store, lazilyLoadFunc, AggressiveCacheMode.TRACK_CHANGES, false);
            assertThat(numberOfRequest)
                    .isEqualTo(0);
        }
    }

    private static int aggressiveCacheOnLazilyLoadTest(
            IDocumentStore store, BiConsumer<IDocumentSession, String> loadFunc, AggressiveCacheMode aggressiveCacheMode, boolean createVersion2) throws Exception {
        String docId = "doc-1";

        try (IDocumentSession session = store.openSession()) {
            Doc doc = new Doc();
            session.store(doc, docId);
            session.saveChanges();
        }

        try (IDocumentSession session = store.openSession()) {
            try (CleanCloseable caching = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5), AggressiveCacheMode.TRACK_CHANGES)) {
                loadFunc.accept(session, docId);
            }
        }

        if (createVersion2) {
            Semaphore mre = new Semaphore(0);

            IDatabaseChanges changes = store.changes();
            changes.ensureConnectedNow();
            IChangesObservable<DocumentChange> observable = changes.forDocument(docId);
            observable.subscribe(Observers.create(x -> {
                mre.release();
            }));

            try (IDocumentSession session = store.openSession()) {
                Doc doc = new Doc();
                doc.setVersion("2");
                session.store(doc, docId);
                session.saveChanges();

                assertThat(mre.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }

        try (IDocumentSession session = store.openSession()) {
            try (CleanCloseable caching = session.advanced().getDocumentStore().aggressivelyCacheFor(Duration.ofMinutes(5), aggressiveCacheMode)) {
                session.advanced().lazily().load(Doc.class, docId).getValue();

                return session.advanced().getNumberOfRequests();
            }
        }
    }
    public static class Doc {
        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    @Test
    public void lazilyLoad_WhenOneOfLoadedIsCachedAndNotModified_ShouldNotBeNull() throws Exception {
        String cachedId = "TestObj/cached";
        String notCachedId = "TestObjs/notCached";

        try (IDocumentStore store = getDocumentStore()) {
            store.aggressivelyCacheFor(Duration.ofMinutes(5));

            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj(), cachedId);
                session.store(new TestObj(), notCachedId);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // load to cache
                session.advanced().lazily().load(TestObj.class, cachedId).getValue();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<TestObj> lazy = session.advanced().lazily().load(TestObj.class, cachedId);
                session.advanced().lazily().load(TestObj.class, notCachedId).getValue();
                assertThat(lazy.getValue())
                        .isNotNull();
            }
        }
    }

    @Test
    public void lazilyLoad_WhenQueryForNotFoundNotModified_ShouldUseCache() throws Exception {
        String notExistDocId = "NotExistDocId";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                //Add "NotExistDocId" to cache
                session.advanced().lazily().load(TestObj.class, notExistDocId).getValue();
            }

            RequestExecutor requestExecutor = store.getRequestExecutor();
            try (IDocumentSession session = store.openSession()) {
                MultiGetOperation multiGetOperation = new MultiGetOperation((InMemoryDocumentSessionOperations) session);
                GetRequest getRequest = new GetRequest();
                getRequest.setUrl("/docs");
                getRequest.setQuery("?&id=" + UrlUtils.escapeDataString(notExistDocId));

                List<GetRequest> requests = Collections.singletonList(getRequest);

                try (MultiGetCommand multiGetCommand = multiGetOperation.createRequest(requests)) {
                    //Should use the cache here and release it in after that
                    requestExecutor.execute(multiGetCommand);
                    assertThat(multiGetCommand.getResult().get(0).getStatusCode())
                            .isEqualTo(HttpStatus.SC_NOT_MODIFIED);
                }
            }
        }
    }

    public static class TestObj {
        private String id;
        private String largeContent;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLargeContent() {
            return largeContent;
        }

        public void setLargeContent(String largeContent) {
            this.largeContent = largeContent;
        }
    }
}
