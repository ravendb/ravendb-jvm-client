package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TrackEntityTest extends RemoteTestBase {
    @Test
    public void deletingEntityThatIsNotTrackedShouldThrow() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> {
                    session.delete(new User());
                })
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("is not associated with the session, cannot delete unknown entity instance");
            }
        }
    }

    @Test
    public void loadingDeletedDocumentShouldReturnNull() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("John");
                user1.setId("users/1");

                User user2 = new User();
                user2.setName("Jonathan");
                user2.setId("users/2");

                session.store(user1);
                session.store(user2);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete("users/1");
                session.delete("users/2");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(User.class, "users/1"))
                        .isNull();
                assertThat(session.load(User.class, "users/2"))
                        .isNull();
            }
        }
    }

    @Test
    public void storingDocumentWithTheSameIdInTheSameSessionShouldThrow() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setId("users/1");
                user.setName("User1");

                session.store(user);
                session.saveChanges();

                final User newUser = new User();
                newUser.setName("User2");
                newUser.setId("users/1");

                assertThatThrownBy(() -> session.store(newUser))
                        .isExactlyInstanceOf(NonUniqueObjectException.class)
                        .hasMessageStartingWith("Attempted to associate a different object with id 'users/1'");
            }
        }
    }
}
