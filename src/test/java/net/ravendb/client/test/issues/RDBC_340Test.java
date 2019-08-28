package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RDBC_340Test extends RemoteTestBase {

    @Override
    protected void customizeStore(DocumentStore store) {
        DocumentConventions conventions = store.getConventions();

        conventions.setFindJavaClassByName(name -> {
            if (User.class.getCanonicalName().equals(name)) {
                return User.class;
            } else {
                throw new RuntimeException("No such class: " + name);
            }
        });
    }

    @Test
    public void canOverrideClassProvider() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Marcin");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User loadedUser = session.load(User.class, "users/1");
                assertThat(loadedUser)
                        .isNotNull();
            }
        }
    }
}
