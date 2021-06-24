package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.infrastructure.orders.Product;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16262Test extends RemoteTestBase {

    @Test
    public void canIncludeCountersInSubscriptions_EvenIfTheyDoNotExist() throws Exception {
        try (IDocumentStore stores = getDocumentStore()) {
            try (IDocumentSession session = stores.openSession()) {
                Product product = new Product();
                session.store(product, "products/1");
                session.saveChanges();
            }

            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setIncludes(builder -> builder.includeCounter("likes").includeCounter("dislikes"));

            String name = stores.subscriptions().create(Product.class, creationOptions);

            try (SubscriptionWorker<Product> sub = stores.subscriptions().getSubscriptionWorker(Product.class, name)) {
                Semaphore semaphore = new Semaphore(0);

                sub.run(batch -> {
                    assertThat(batch.getItems().size())
                            .isPositive();

                    try (IDocumentSession s = batch.openSession()) {
                        for (SubscriptionBatch.Item<Product> item : batch.getItems()) {
                            Product product = s.load(Product.class, item.getId());
                            assertThat(item.getResult())
                                    .isSameAs(product);

                            Long likesValues = s.countersFor(product)
                                    .get("likes");
                            assertThat(likesValues)
                                    .isNull();

                            Long dislikesValue = s.countersFor(product).get("dislikes");
                            assertThat(dislikesValue)
                                    .isNull();
                        }

                        assertThat(s.advanced().getNumberOfRequests())
                                .isEqualTo(0);
                    }

                    semaphore.release();
                });

                assertThat(semaphore.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }
}
