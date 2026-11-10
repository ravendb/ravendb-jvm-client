package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_22345Test extends RemoteTestBase {

    @Test
    public void concurrencyExceptionContainsServerErrorAsCause() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "A:1-invalidChangeVector", "users/1");

                assertThatThrownBy(session::saveChanges)
                        .isExactlyInstanceOf(ConcurrencyException.class)
                        .hasCauseExactlyInstanceOf(RavenException.class)
                        .satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.getCause().getMessage()).isNotEmpty());
            }
        }
    }
}
