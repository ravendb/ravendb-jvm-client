package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteTest extends RemoteTestBase {

    @Test
    public void deleteDocumentByEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                newSession.store(user, "users/1");
                newSession.saveChanges();

                user = newSession.load(User.class, "users/1");

                assertThat(user)
                        .isNotNull();

                newSession.delete(user);
                newSession.saveChanges();

                User nullUser = newSession.load(User.class, "users/1");
                assertThat(nullUser)
                        .isNull();

            }
        }
    }

    @Test
    public void deleteDocumentById() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                newSession.store(user, "users/1");
                newSession.saveChanges();

                user = newSession.load(User.class, "users/1");

                assertThat(user)
                        .isNotNull();

                newSession.delete("users/1");
                newSession.saveChanges();

                User nullUser = newSession.load(User.class, "users/1");
                assertThat(nullUser)
                        .isNull();

            }
        }
    }
}
