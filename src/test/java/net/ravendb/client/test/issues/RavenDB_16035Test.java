package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16035Test extends RemoteTestBase {

    private boolean clearCache = false;

    public static class User {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Item {

    }

    @Override
    protected void customizeStore(DocumentStore store) {
        store.addOnSucceedRequestListener((sender, event) -> {
            if (clearCache) {
                store.getRequestExecutor().getCache().clear();
            }
        });
    }

    @Test
    public void canMixLazyAndAggressiveCaching() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Arava");
                session.store(user);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<List<User>> l1 = session.query(User.class)
                        .whereEquals("name", "Arava")
                        .lazily();
                Lazy<List<User>> l2 = session.query(User.class)
                        .whereEquals("name", "Phoebe")
                        .lazily();
                Lazy<Integer> l3 = session.query(User.class)
                        .whereExists("name")
                        .countLazily();

                assertThat(l1.getValue())
                        .isNotEmpty();
                assertThat(l2.getValue())
                        .isEmpty();
                assertThat(l3.getValue())
                        .isEqualTo(1);
            }

            try (IDocumentSession session = store.openSession()) {
                clearCache = true;

                Lazy<List<User>> l1 = session.query(User.class)
                        .whereEquals("name", "Arava")
                        .lazily();
                Lazy<List<User>> l2 = session.query(User.class)
                        .whereEquals("name", "Phoebe")
                        .lazily();
                Lazy<Integer> l3 = session.query(User.class)
                        .whereExists("name")
                        .countLazily();

                assertThat(l1.getValue())
                        .isNotEmpty();
                assertThat(l2.getValue())
                        .isEmpty();
                assertThat(l3.getValue())
                        .isEqualTo(1);
            }
        }
    }
}
