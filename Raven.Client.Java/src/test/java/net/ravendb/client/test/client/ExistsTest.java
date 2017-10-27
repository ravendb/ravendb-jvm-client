package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExistsTest extends RemoteTestBase {
    @Test
    public void checkIfDocumentExists() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User idan = new User();
                idan.setName("Idan");

                User shalom = new User();
                shalom.setName("Shalom");

                session.store(idan, "users/1");
                session.store(shalom, "users/2");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().exists("users/1"))
                        .isTrue();
                assertThat(session.advanced().exists("users/10"))
                        .isFalse();

                session.load(User.class, "users/2");
                assertThat(session.advanced().exists("users/2"))
                        .isTrue();
            }
        }
    }
}
