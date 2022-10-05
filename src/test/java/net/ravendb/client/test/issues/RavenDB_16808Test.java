package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.AggressiveCacheMode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16808Test extends RemoteTestBase {

    @Test
    public void shouldIncrementOnlySessionAdvancedNumberOfRequests() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            RequestExecutor requestExecutor = store.getRequestExecutor();
            User entity = new User();
            try (IDocumentSession session = store.openSession()) {
                session.store(entity);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.load(User.class, entity.getId());
            }

            try (IDocumentSession session = store.openSession()) {
                try (CleanCloseable cache = store.aggressivelyCacheFor(Duration.ofDays(1), AggressiveCacheMode.DO_NOT_TRACK_CHANGES)) {
                    AtomicLong reBefore = requestExecutor.numberOfServerRequests;
                    int sessionBefore = session.advanced().getNumberOfRequests();

                    session.load(User.class, entity.getId());

                    long reForLoad = requestExecutor.numberOfServerRequests.get() - reBefore.get(); // We took the value from cache
                    int sessionForLoad = session.advanced().getNumberOfRequests() - sessionBefore;

                    assertThat(sessionForLoad)
                            .isEqualTo(1);
                    assertThat(reForLoad)
                            .isZero();
                }
            }
        }
    }
}
