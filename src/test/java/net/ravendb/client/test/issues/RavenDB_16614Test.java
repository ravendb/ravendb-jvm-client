package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.ClusterTransactionConcurrencyException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_16614Test extends RemoteTestBase {

    @Test
    public void modificationInAnotherTransactionWillFailWithDelete() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User user1 = new User();
                user1.setName("arava");

                User user2 = new User();
                user2.setName("phoebe");

                session.store(user1, "users/arava");
                session.store(user2, "users/phoebe");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                User user = session.load(User.class, "users/arava");
                session.delete(user);
                User user2 = session.load(User.class, "users/phoebe");
                user2.setName("Phoebe Eini");

                try (IDocumentSession conflictedSession = store.openSession(sessionOptions)) {
                    User conflictedArava = conflictedSession.load(User.class, "users/arava");
                    conflictedArava.setName("Arava!");
                    conflictedSession.saveChanges();
                }

                assertThatThrownBy(() -> {
                    session.saveChanges();
                })
                        .isInstanceOf(ClusterTransactionConcurrencyException.class);
            }
        }
    }

}
