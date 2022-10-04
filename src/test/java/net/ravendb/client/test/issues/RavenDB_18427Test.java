package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_18427Test extends RemoteTestBase {

    @Test
    public void store_documents2() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Foo/Bar");
                session.store(user, "foo");
                session.store(user, "Foo");

                assertThatThrownBy(() -> session.store(user, "bar"))
                        .isExactlyInstanceOf(IllegalStateException.class);
                session.saveChanges();

                int usersCount = session.query(User.class)
                        .count();

                assertThat(usersCount)
                        .isEqualTo(1);

                User user1 = session.load(User.class, "foo");
                assertThat(user1)
                        .isNotNull();
                User user2 = session.load(User.class, "bar");
                assertThat(user2)
                        .isNull();
            }
        }
    }
}
