package net.ravendb.client.documents.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeleteDocumentCommandTest extends RemoteTestBase {

    @Test
    public void canDeleteDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Marcin");
                session.store(user, "users/1");
                session.saveChanges();
            }
            DeleteDocumentCommand command = new DeleteDocumentCommand("users/1");
            store.getRequestExecutor().execute(command);

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");
                assertThat(loadedUser)
                        .isNull();
            }
        }
    }

    @Test
    public void canDeleteDocumentByEtag() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            String changeVector = null;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Marcin");
                session.store(user, "users/1");
                session.saveChanges();

                changeVector = session.advanced().getChangeVectorFor(user);
            }

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");
                loadedUser.setAge(5);
                session.saveChanges();
            }
            DeleteDocumentCommand command = new DeleteDocumentCommand("users/1", changeVector);
            assertThatThrownBy(() -> {
                store.getRequestExecutor().execute(command);
            }).isExactlyInstanceOf(ConcurrencyException.class);
        }
    }


}
