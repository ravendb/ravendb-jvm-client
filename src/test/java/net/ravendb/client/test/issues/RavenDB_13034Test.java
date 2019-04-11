package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.exceptions.documents.indexes.IndexDoesNotExistException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_13034Test extends RemoteTestBase {

    public static class User {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    @Test
    public void exploringConcurrencyBehavior() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession s1 = store.openSession()) {
                User user = new User();
                user.setName("Nick");
                user.setAge(99);
                s1.store(user, "users/1-A");
                s1.saveChanges();
            }

            try (IDocumentSession s2 = store.openSession()) {
                s2.advanced().setUseOptimisticConcurrency(true);

                User u2 = s2.load(User.class, "users/1-A");

                try (IDocumentSession s3 = store.openSession()) {
                    User u3 = s3.load(User.class, "users/1-A");
                    assertThat(u2)
                            .isNotEqualTo(u3);

                    u3.age--;
                    s3.saveChanges();
                }

                u2.age++;

                User u2_2 = s2.load(User.class, "users/1-A");
                assertThat(u2)
                        .isEqualTo(u2_2);
                assertThat(s2.advanced().getNumberOfRequests())
                        .isOne();

                assertThatThrownBy(() -> s2.saveChanges())
                        .isExactlyInstanceOf(ConcurrencyException.class);

                assertThat(s2.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                User u2_3 = s2.load(User.class, "users/1-A");
                assertThat(u2)
                        .isEqualTo(u2_3);
                assertThat(s2.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                assertThatThrownBy(() -> s2.saveChanges())
                        .isExactlyInstanceOf(ConcurrencyException.class);
            }

            try (IDocumentSession s4 = store.openSession()) {
                User u4 = s4.load(User.class, "users/1-A");
                assertThat(u4.getAge())
                        .isEqualTo(98);
            }
        }
    }
}
