package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_20831Test extends RemoteTestBase {

    @Test
    public void canUseLongCountLazily() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 3; i++) {
                    User user = new User();
                    user.setName("John" + i);
                    session.store(user);
                }
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<Long> count = session.query(User.class)
                        .waitForNonStaleResults()
                        .longCountLazily();
                Lazy<Long> rawCount = session.advanced().rawQuery(User.class, "from Users where name = 'John1'")
                        .waitForNonStaleResults()
                        .longCountLazily();

                assertThat(count.isValueCreated())
                        .isFalse();
                assertThat(session.advanced().getNumberOfRequests())
                        .isZero();

                assertThat(count.getValue())
                        .isEqualTo(3L);
                assertThat(rawCount.getValue())
                        .isEqualTo(1L);
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
            }
        }
    }
}
