package net.ravendb.client.test.client.queries;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QueryTagTest extends RemoteTestBase {

    @Test
    public void tagIsSetOnIndexQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<User> query = session.advanced().documentQuery(User.class)
                        .withTag("my-tag");

                assertThat(query.getIndexQuery().getTag())
                        .isEqualTo("my-tag");
            }
        }
    }

    @Test
    public void tagIsSetOnRawQueryIndexQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<User> query = session.advanced().rawQuery(User.class, "from Users")
                        .withTag("raw-tag");

                assertThat(query.getIndexQuery().getTag())
                        .isEqualTo("raw-tag");
            }
        }
    }

    @Test
    public void blankTagThrows() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.advanced().documentQuery(User.class).withTag(" "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Query tag cannot be null or whitespace");

                assertThatThrownBy(() -> session.advanced().documentQuery(User.class).withTag(null))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    @EnableOnServer(thresholdVersion = "7.2.2")
    public void queryWithTagExecutes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Aviv");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().documentQuery(User.class)
                        .withTag("tagged-query")
                        .whereEquals("name", "Aviv")
                        .toList())
                        .hasSize(1);
            }
        }
    }
}
