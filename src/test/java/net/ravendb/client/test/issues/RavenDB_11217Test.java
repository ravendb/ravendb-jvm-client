package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.infrastructure.orders.Product;
import net.ravendb.client.infrastructure.orders.Supplier;
import net.ravendb.client.primitives.Reference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_11217Test extends RemoteTestBase {

    @Test
    public void sessionWideNoTrackingShouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Supplier supplier = new Supplier();
                supplier.setName("Supplier1");

                session.store(supplier);

                Product product = new Product();
                product.setName("Product1");
                product.setSupplier(supplier.getId());

                session.store(product);
                session.saveChanges();
            }

            SessionOptions noTrackingOptions = new SessionOptions();
            noTrackingOptions.setNoTracking(true);

            try (IDocumentSession session = store.openSession(noTrackingOptions)) {
                Supplier supplier = new Supplier();
                supplier.setName("Supplier2");

                assertThatThrownBy(() -> session.store(supplier))
                        .isInstanceOf(IllegalStateException.class);
            }

            try (IDocumentSession session = store.openSession(noTrackingOptions)) {
                assertThat(session.advanced().getNumberOfRequests())
                        .isZero();

                Product product1 = session.load(Product.class, "products/1-A", b -> b.includeDocuments("supplier"));

                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();

                assertThat(product1)
                        .isNotNull();
                assertThat(product1.getName())
                        .isEqualTo("Product1");
                assertThat(session.advanced().isLoaded(product1.getId()))
                        .isFalse();
                assertThat(session.advanced().isLoaded(product1.getSupplier()))
                        .isFalse();

                Supplier supplier = session.load(Supplier.class, product1.getSupplier());
                assertThat(supplier)
                        .isNotNull();
                assertThat(supplier.getName())
                        .isEqualTo("Supplier1");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(session.advanced().isLoaded(supplier.getId()))
                        .isFalse();

                Product product2 = session.load(Product.class, "products/1-A", b -> b.includeDocuments("supplier"));
                assertThat(product1)
                        .isNotSameAs(product2);
            }

            try (IDocumentSession session = store.openSession(noTrackingOptions)) {
                assertThat(session.advanced().getNumberOfRequests())
                        .isZero();

                Product product1 = session
                        .advanced()
                        .loadStartingWith(Product.class, "products/")[0];

                assertThat(session.advanced().getNumberOfRequests())
                        .isOne();

                assertThat(product1)
                        .isNotNull();
                assertThat(product1.getName())
                        .isEqualTo("Product1");
                assertThat(session.advanced().isLoaded(product1.getId()))
                        .isFalse();
                assertThat(session.advanced().isLoaded(product1.getSupplier()))
                        .isFalse();

                Supplier supplier = session.advanced().loadStartingWith(Supplier.class, product1.getSupplier())[0];

                assertThat(supplier)
                        .isNotNull();
                assertThat(supplier.getName())
                        .isEqualTo("Supplier1");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(session.advanced().isLoaded(supplier.getId()))
                        .isFalse();

                Product product2 = session
                        .advanced()
                        .loadStartingWith(Product.class, "products/")[0];

                assertThat(product1)
                        .isNotSameAs(product2);
            }

            try (IDocumentSession session = store.openSession(noTrackingOptions)) {
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(0);

                List<Product> products = session
                        .query(Product.class)
                        .include("supplier")
                        .toList();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(1);
                assertThat(products)
                        .hasSize(1);

                Product product1 = products.get(0);
                assertThat(product1)
                        .isNotNull();
                assertThat(session.advanced().isLoaded(product1.getId()))
                        .isFalse();
                assertThat(session.advanced().isLoaded(product1.getSupplier()))
                        .isFalse();

                Supplier supplier = session.load(Supplier.class, product1.getSupplier());
                assertThat(supplier)
                        .isNotNull();
                assertThat(supplier.getName())
                        .isEqualTo("Supplier1");
                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);
                assertThat(session.advanced().isLoaded(supplier.getId()))
                        .isFalse();

                products = session
                        .query(Product.class)
                        .include("supplier")
                        .toList();

                assertThat(products)
                        .hasSize(1);

                Product product2 = products.get(0);
                assertThat(product1)
                        .isNotSameAs(product2);
            }

            try (IDocumentSession session = store.openSession()) {
                session.countersFor("products/1-A").increment("c1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(noTrackingOptions)) {
                Product product1 = session.load(Product.class, "products/1-A");
                ISessionDocumentCounters counters = session.countersFor(product1.getId());

                counters.get("c1");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(2);

                counters.get("c1");

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(3);

                Map<String, Long> val1 = counters.getAll();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(4);

                Map<String, Long> val2 = counters.getAll();

                assertThat(session.advanced().getNumberOfRequests())
                        .isEqualTo(5);

                assertThat(val1)
                        .isNotSameAs(val2);
            }

        }
    }

    @Test
    public void sessionWideNoCachingShouldWork() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                session.query(Product.class)
                        .statistics(statsRef)
                        .whereEquals("name", "HR")
                        .toList();

                assertThat(statsRef.value.getDurationInMs())
                        .isPositive();

                session.query(Product.class)
                        .statistics(statsRef)
                        .whereEquals("name", "HR")
                        .toList();

                assertThat(statsRef.value.getDurationInMs())
                        .isEqualTo(-1);  // from cache
            }

            SessionOptions noCacheOptions = new SessionOptions();
            noCacheOptions.setNoCaching(true);

            try (IDocumentSession session = store.openSession(noCacheOptions)) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                session.query(Product.class)
                        .statistics(statsRef)
                        .whereEquals("name", "HR")
                        .toList();

                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative();

                session.query(Product.class)
                        .statistics(statsRef)
                        .whereEquals("name", "HR")
                        .toList();

                assertThat(statsRef.value.getDurationInMs())
                        .isNotNegative();
            }
        }
    }
}
