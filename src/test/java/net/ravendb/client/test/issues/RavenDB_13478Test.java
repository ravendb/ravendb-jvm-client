package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.orders.Product;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_13478Test extends RemoteTestBase {

    @Test
    public void canIncludeCountersInSubscriptions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Product product = new Product();
                session.store(product);

                session.countersFor(product).increment("likes", 3);
                session.countersFor(product).increment("dislikes", 5);

                session.saveChanges();
            }

            SubscriptionCreationOptions options = new SubscriptionCreationOptions();
            options.setIncludes(builder -> {
                builder.includeCounter("likes")
                        .includeCounter("dislikes");
            });

            String name = store.subscriptions().create(Product.class, options);

            assertSubscription(store, name, 0);

            options = new SubscriptionCreationOptions();
            options.setIncludes(builder -> builder.includeAllCounters());

            name = store.subscriptions().create(Product.class, options);

            assertSubscription(store, name, 0);

            options = new SubscriptionCreationOptions();
            options.setIncludes(builder -> {
                builder.includeCounter("likes");
            });

            name = store.subscriptions().create(Product.class, options);

            assertSubscription(store, name, 1);

            name = store.subscriptions().create(Product.class);

            assertSubscription(store, name, 2);
        }
    }

    public static void assertSubscription(IDocumentStore store, String name, int expectedNumberOfRequests) throws Exception {
        try (SubscriptionWorker<Product> sub
                     = store.subscriptions().getSubscriptionWorker(Product.class, new SubscriptionWorkerOptions(name))) {
            Semaphore semaphore = new Semaphore(0);
            CompletableFuture<Void> t = sub.run(batch -> {
                assertThat(batch.getItems())
                        .isNotEmpty();

                try (IDocumentSession s = batch.openSession()) {
                    for (SubscriptionBatch.Item<Product> item : batch.getItems()) {
                        Product product = s.load(Product.class, item.getId());
                        assertThat(item.getResult())
                                .isSameAs(product);

                        Long likesValue = s.countersFor(product).get("likes");
                        assertThat(likesValue)
                                .isEqualTo(3);

                        Long dislikesValue = s.countersFor(product).get("dislikes");
                        assertThat(dislikesValue)
                                .isEqualTo(5);
                    }

                    assertThat(s.advanced().getNumberOfRequests())
                            .isEqualTo(expectedNumberOfRequests);
                }

                semaphore.release();
            });

            semaphore.tryAcquire(30, TimeUnit.SECONDS);
        }
    }
}
