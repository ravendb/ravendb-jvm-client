package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17551Test extends RemoteTestBase {

    @Test
    public void canUseOffsetWithCollectionQuery() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 5; i++) {
                    User user = new User();
                    user.setName("i = " + i);
                    session.store(user);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.query(User.class).take(3).skip(2).toList().size())
                        .isEqualTo(3);
                assertThat(session.query(User.class).take(3).skip(3).toList().size())
                        .isEqualTo(2);
            }
        }
    }
}
