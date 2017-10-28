package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class StoreTest extends RemoteTestBase {


    @Test
    public void storeDocument() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                session.store(user, "users/1");
                session.saveChanges();

                user = session.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
                assertThat(user.getName())
                        .isEqualTo("RavenDB");
            }
        }
    }

    @Test
    public void storeDocuments() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("RavenDB");
                session.store(user1, "users/1");

                User user2 = new User();
                user2.setName("Hibernating Rhinos");
                session.store(user2, "users/2");

                session.saveChanges();

                Map<String, User> users = session.load(User.class, "users/1", "users/2");
                assertThat(users)
                        .hasSize(2);
            }
        }
    }
}
