package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.changes.DocumentChange;
import net.ravendb.client.documents.changes.IChangesObservable;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.changes.Observers;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_19945Test extends RemoteTestBase {

    @Test
    public void willNotSendEvenForEachChangeForAggressiveCaching() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            int count = 100;

            Semaphore semaphore = new Semaphore(0);

            RequestExecutor requestExecutor = store.getRequestExecutor();
            int cacheGeneration = requestExecutor.getCache().generation.get();

            try (CleanCloseable aggressivelyCache = store.aggressivelyCache()) {
                IDatabaseChanges databaseChanges = store.changes();
                databaseChanges.ensureConnectedNow();
                IChangesObservable<DocumentChange> forAllDocuments = databaseChanges.forAllDocuments();
                forAllDocuments.subscribe(new Observers.ActionBasedObserver<>((item) -> semaphore.release()));

                for (int i = 0; i < count / 10; i++) {
                    try (IDocumentSession session = store.openSession()) {

                        for (int j = 0; j < count / 10; j++) {
                            session.store(new User());
                        }

                        session.saveChanges();
                    }
                }

                assertThat(semaphore.tryAcquire(100, 30, TimeUnit.SECONDS))
                        .isTrue();

                assertThat(requestExecutor.getCache().generation.get())
                        .isNotEqualTo(cacheGeneration); // has updates

                assertThat(cacheGeneration + count > requestExecutor.getCache().generation.get())
                        .isTrue(); // not for every single one
            }
        }
    }
}
