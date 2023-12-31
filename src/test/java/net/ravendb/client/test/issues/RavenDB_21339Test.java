package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.infrastructure.orders.Product;
import net.ravendb.client.infrastructure.orders.Supplier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_21339Test extends RemoteTestBase {

    @Test
    public void using_Includes_In_Non_Tracking_Session_Should_Throw() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Supplier supplier = new Supplier();
                supplier.setId("suppliers/1");

                session.store(supplier);
                Product product = new Product();
                product.setId("products/1");
                product.setSupplier(supplier.getId());
                session.store(product);

                session.saveChanges();
            }

            SessionOptions noTracking = new SessionOptions();
            noTracking.setNoTracking(true);
            try (IDocumentSession session = store.openSession(noTracking)) {

                assertThatThrownBy(() -> {
                    session.load(Product.class, "products/1", includes -> includes.includeDocuments("supplier"));
                }).hasMessageContaining("registering includes is forbidden");

                assertThatThrownBy(() -> {
                    session.query(Product.class)
                            .include("supplier")
                            .toList();
                })
                        .hasMessageContaining("registering includes is forbidden");
            }
        }
    }
}
