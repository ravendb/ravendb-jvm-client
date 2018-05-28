package net.ravendb.client.test.client.lazy;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LazyTest extends RemoteTestBase {

    @Test
    public void canLazilyLoadEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 1; i <= 6; i++) {
                    Company company = new Company();
                    company.setId("companies/" + i);
                    session.store(company, "companies/" + i);
                }

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Lazy<Company> lazyOrder = session.advanced().lazily().load(Company.class, "companies/1");
                assertThat(lazyOrder.isValueCreated())
                        .isFalse();

                Company order = lazyOrder.getValue();
                assertThat(order.getId())
                        .isEqualTo("companies/1");

                Lazy<Map<String, Company>> lazyOrders = session.advanced().lazily().load(Company.class, Arrays.asList("companies/1", "companies/2"));
                assertThat(lazyOrders.isValueCreated())
                        .isFalse();

                Map<String, Company> orders = lazyOrders.getValue();
                assertThat(orders)
                        .hasSize(2);

                Company company1 = orders.get("companies/1");
                Company company2 = orders.get("companies/2");

                assertThat(company1)
                        .isNotNull();

                assertThat(company2)
                        .isNotNull();

                assertThat(company1.getId())
                        .isEqualTo("companies/1");

                assertThat(company2.getId())
                        .isEqualTo("companies/2");

                lazyOrder = session.advanced().lazily().load(Company.class, "companies/3");

                assertThat(lazyOrder.isValueCreated())
                        .isFalse();

                order = lazyOrder.getValue();

                assertThat(order.getId())
                        .isEqualTo("companies/3");

                Lazy<Map<String, Company>> load = session.advanced().lazily().load(Company.class, Arrays.asList("no_such_1", "no_such_2"));
                Map<String, Company> missingItems = load.getValue();

                assertThat(missingItems.get("no_such_1"))
                        .isNull();

                assertThat(missingItems.get("no_such_2"))
                        .isNull();

            }
        }
    }

    @Test
    public void canExecuteAllPendingLazyOperations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setId("companies/1");
                session.store(company1, "companies/1");

                Company company2 = new Company();
                company2.setId("companies/2");
                session.store(company2, "companies/2");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Reference<Company> company1Ref = new Reference<>();
                Reference<Company> company2Ref = new Reference<>();
                session.advanced().lazily().load(Company.class, "companies/1", x -> company1Ref.value = x);
                session.advanced().lazily().load(Company.class, "companies/2", x -> company2Ref.value = x);

                assertThat(company1Ref.value)
                        .isNull();

                assertThat(company2Ref.value)
                        .isNull();

                session.advanced().eagerly().executeAllPendingLazyOperations();

                assertThat(company1Ref.value)
                        .isNotNull();
                assertThat(company2Ref.value)
                        .isNotNull();

                assertThat(company1Ref.value.getId())
                        .isEqualTo("companies/1");
                assertThat(company2Ref.value.getId())
                        .isEqualTo("companies/2");
            }
        }
    }

    @Test
    public void withQueuedActions_Load() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Oren");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Reference<User> userRef = new Reference<>();

                session.advanced().lazily().load(User.class, "users/1", x -> userRef.value = x);

                session.advanced().eagerly().executeAllPendingLazyOperations();

                assertThat(userRef.value)
                        .isNotNull();
            }
        }
    }

    @Test
    public void canUseCacheWhenLazyLoading() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setLastName("Oren");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().lazily().load(User.class, "users/1").getValue();
            }
            try (IDocumentSession session = store.openSession()) {
                User user = session.advanced().lazily().load(User.class, "users/1").getValue();
                assertThat(user.getLastName())
                        .isEqualTo("Oren");
            }
        }
    }
}
