package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.ConditionalLoadResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConditionalLoadTest extends RemoteTestBase {

    @Test
    public void conditionalLoad_CanGetDocumentById() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                session.store(user, "users/1");
                session.saveChanges();
            }

            String cv;
            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                cv = newSession.advanced().getChangeVectorFor(user);
                assertThat(user)
                        .isNotNull();
                assertThat(user.getName())
                        .isEqualTo("RavenDB");
                user.setName("RavenDB 5.1");
                newSession.saveChanges();
            }

            try (IDocumentSession newestSession = store.openSession()) {
                ConditionalLoadResult<User> user = newestSession.advanced().conditionalLoad(User.class, "users/1", cv);
                assertThat(user.getEntity().getName())
                        .isEqualTo("RavenDB 5.1");
                assertThat(user.getChangeVector())
                        .isNotNull()
                        .isNotEqualTo(cv);
            }
        }
    }

    @Test
    public void conditionalLoad_GetNotModifiedDocumentByIdShouldReturnNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                session.store(user, "users/1");
                session.saveChanges();
            }

            String cv;

            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
                assertThat(user.getName())
                        .isEqualTo("RavenDB");
                user.setName("RavenDB 5.1");
                newSession.saveChanges();
                cv = newSession.advanced().getChangeVectorFor(user);
            }

            try (IDocumentSession newestSession = store.openSession()) {
                ConditionalLoadResult<User> user = newestSession.advanced().conditionalLoad(User.class, "users/1", cv);
                assertThat(user.getEntity())
                        .isNull();
                assertThat(user.getChangeVector())
                        .isEqualTo(cv);
            }
        }
    }

    @Test
    public void conditionalLoad_NonExistsDocumentShouldReturnNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("RavenDB");
                session.store(user, "users/1");
                session.saveChanges();
            }

            String cv;
            try (IDocumentSession newSession = store.openSession()) {
                User user = newSession.load(User.class, "users/1");
                assertThat(user)
                        .isNotNull();
                assertThat(user.getName())
                        .isEqualTo("RavenDB");
                user.setName("RavenDB 5.1");
                newSession.saveChanges();
                cv = newSession.advanced().getChangeVectorFor(user);
            }

            try (IDocumentSession newestSession = store.openSession()) {
                assertThatThrownBy(() -> {
                    newestSession.advanced().conditionalLoad(User.class, "users/2", null);
                }).isExactlyInstanceOf(IllegalArgumentException.class);

                ConditionalLoadResult<User> result = newestSession.advanced().conditionalLoad(User.class, "users/2", cv);
                assertThat(result.getEntity())
                        .isNull();
                assertThat(result.getChangeVector())
                        .isNull();

                assertThat(newestSession.advanced().isLoaded("users/2"))
                        .isTrue();

                int expected = newestSession.advanced().getNumberOfRequests();
                newestSession.load(User.class, "users/2");

                assertThat(newestSession.advanced().getNumberOfRequests())
                        .isEqualTo(expected);

            }
        }
    }
}
