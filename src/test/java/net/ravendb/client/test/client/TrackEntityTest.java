package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.DocumentsById;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.session.NonUniqueObjectException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TrackEntityTest extends RemoteTestBase {
    @Test
    public void deletingEntityThatIsNotTrackedShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.delete(new User()))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageEndingWith("is not associated with the session, cannot delete unknown entity instance");
            }
        }
    }

    @Test
    public void loadingDeletedDocumentShouldReturnNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("John");
                user1.setId("users/1");

                User user2 = new User();
                user2.setName("Jonathan");
                user2.setId("users/2");

                session.store(user1);
                session.store(user2);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete("users/1");
                session.delete("users/2");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(User.class, "users/1"))
                        .isNull();
                assertThat(session.load(User.class, "users/2"))
                        .isNull();
            }
        }
    }

    @Test
    public void storingDocumentWithTheSameIdInTheSameSessionShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setId("users/1");
                user.setName("User1");

                session.store(user);
                session.saveChanges();

                final User newUser = new User();
                newUser.setName("User2");
                newUser.setId("users/1");

                assertThatThrownBy(() -> session.store(newUser))
                        .isExactlyInstanceOf(NonUniqueObjectException.class)
                        .hasMessageStartingWith("Attempted to associate a different object with id 'users/1'");
            }
        }
    }

    @Test
    public void getTrackedEntities() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            String userId;
            String companyId;

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Grisha");

                session.store(user);
                userId = user.getId();

                Company company = new Company();
                company.setName("Hibernating Rhinos");
                session.store(company);

                companyId = company.getId();

                Order order = new Order();
                order.setEmployee(company.getId());
                session.store(order);

                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();

                DocumentsById.EntityInfo value = tracked.get(userId);
                assertThat(value)
                        .isNotNull();

                assertThat(value.getId())
                        .isEqualTo(userId);
                assertThat(value.getEntity())
                        .isExactlyInstanceOf(User.class);

                value = tracked.get(company.getId());
                assertThat(value)
                        .isNotNull();
                assertThat(value.getId())
                        .isEqualTo(companyId);
                assertThat(value.getEntity())
                        .isExactlyInstanceOf(Company.class);

                value = tracked.get(order.getId());
                assertThat(value)
                        .isNotNull();
                assertThat(value.getId())
                        .isEqualTo(order.getId());

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete(userId);
                session.delete(companyId);

                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();
                assertThat(tracked)
                        .hasSize(2);
                assertThat(tracked.get(userId).isDeleted())
                        .isTrue();
                assertThat(tracked.get(companyId).isDeleted())
                        .isTrue();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete(userId);
                session.delete(companyId);

                Lazy<Map<String, User>> usersLazy = session.advanced().lazily().loadStartingWith(User.class, "u");
                Map<String, User> users = usersLazy.getValue();
                assertThat(users.entrySet().stream().findFirst().get().getValue())
                        .isNull();

                Company company = session.load(Company.class, companyId);
                assertThat(company)
                        .isNull();

                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();
                assertThat(tracked)
                        .hasSize(2);
                assertThat(tracked.get(userId).isDeleted())
                        .isTrue();
                assertThat(tracked.get(companyId).isDeleted())
                        .isTrue();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, userId);
                session.delete(user.getId());

                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();
                assertThat(tracked)
                        .hasSize(1);
                assertThat(tracked.get(userId).getId())
                        .isEqualTo(userId);
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, userId);
                session.delete(user.getId().toUpperCase());
                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();
                assertThat(tracked)
                        .hasSize(1);
                assertThat(tracked.entrySet().stream().findFirst().get().getKey())
                        .isEqualToIgnoringCase(userId);
                assertThat(tracked.entrySet().stream().findFirst().get().getValue().isDeleted())
                        .isTrue();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, userId);
                session.delete(user);
                Map<String, DocumentsById.EntityInfo> tracked = session.advanced().getTrackedEntities();
                assertThat(tracked)
                        .hasSize(1);
                assertThat(tracked.entrySet().stream().findFirst().get().getKey())
                        .isEqualToIgnoringCase(userId);
                assertThat(tracked.entrySet().stream().findFirst().get().getValue().isDeleted())
                        .isTrue();
            }
        }
    }
}
