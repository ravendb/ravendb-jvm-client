package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class RDBC_316Test extends RemoteTestBase {

    @Test
    public void canStoreEqualDocumentUnderTwoDifferentKeys() throws Exception {

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Marcin");

                User user2 = new User();
                user2.setName("Marcin");

                assertThat(user1)
                        .isEqualTo(user2);

                session.store(user1, "users/1");
                session.store(user2, "users/2");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user1 = session.load(User.class, "users/1");
                User user2 = session.load(User.class, "users/2");

                assertThat(user1)
                        .isNotNull();

                assertThat(user2)
                        .isNotNull();

                assertThat(user1 == user2)
                        .isFalse();
            }
        }


    }

    public static class User {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return Objects.equals(name, user.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
