package net.ravendb.client.test.issues;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15539Test extends RemoteTestBase {

    public static class User {
        private String name;
        private boolean ignoreChanges;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @JsonIgnore
        public boolean isIgnoreChanges() {
            return ignoreChanges;
        }

        public void setIgnoreChanges(boolean ignoreChanges) {
            this.ignoreChanges = ignoreChanges;
        }
    }

    @Override
    protected void customizeStore(DocumentStore store) {
        store.getConventions()
                .setShouldIgnoreEntityChanges((session, entity, id) -> entity instanceof User && ((User) entity).ignoreChanges);
    }

    @Test
    public void canIgnoreChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");
                session.store(user, "users/oren");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/oren");
                user.setName("Arava");
                user.setIgnoreChanges(true);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/oren");
                assertThat(user.getName())
                        .isEqualTo("Oren");
            }

        }
    }
}
