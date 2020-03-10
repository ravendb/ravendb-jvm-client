package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.orders.Category;
import net.ravendb.client.infrastructure.orders.Product;
import net.ravendb.client.infrastructure.orders.Supplier;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_12257Test extends RemoteTestBase {

    private final int _reasonableWaitTime = 60;

    @Test
    public void canUseSubscriptionIncludesViaStronglyTypedApi() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Product product = new Product();
                Category category = new Category();
                Supplier supplier = new Supplier();

                session.store(category);
                session.store(product);

                product.setCategory(category.getId());
                product.setSupplier(supplier.getId());

                session.store(product);

                session.saveChanges();
            }

            SubscriptionCreationOptions options = new SubscriptionCreationOptions();
            options.setIncludes(builder -> {
                builder.includeDocuments("category")
                        .includeDocuments("supplier");
            });
            String name = store.subscriptions().create(Product.class, options);

            try (SubscriptionWorker<Product> sub
                         = store.subscriptions().getSubscriptionWorker(Product.class, new SubscriptionWorkerOptions(name))) {

                Semaphore semaphore = new Semaphore(0);
                CompletableFuture<Void> t = sub.run(batch -> {
                    assertThat(batch.getItems())
                            .isNotEmpty();

                    try (IDocumentSession s = batch.openSession()) {
                        for (SubscriptionBatch.Item<Product> item : batch.getItems()) {
                            s.load(Category.class, item.getResult().getCategory());
                            s.load(Supplier.class, item.getResult().getSupplier());
                            Product product = s.load(Product.class, item.getId());
                            assertThat(product)
                                    .isSameAs(item.getResult());
                        }
                        assertThat(s.advanced().getNumberOfRequests())
                                .isZero();
                    }

                    semaphore.release();
                });

                semaphore.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS);

            }
        }
    }
}
