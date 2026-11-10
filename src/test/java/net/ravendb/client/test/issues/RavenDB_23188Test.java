package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_23188Test extends RemoteTestBase {

    @Test
    public void onSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setNoCaching(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                session.query(User.class).toList();

                assertThat(session.advanced().getRequestExecutor().getCache().getNumberOfItems())
                        .isZero();
            }
        }

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession(new SessionOptions())) {
                session.query(User.class).toList();

                assertThat(session.advanced().getRequestExecutor().getCache().getNumberOfItems())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void onQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.advanced().documentQuery(User.class)
                        .noCaching()
                        .toList();

                assertThat(session.advanced().getRequestExecutor().getCache().getNumberOfItems())
                        .isZero();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().documentQuery(User.class)
                        .toList();

                assertThat(session.advanced().getRequestExecutor().getCache().getNumberOfItems())
                        .isEqualTo(1);
            }
        }
    }
}
