package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_19538Test extends RemoteTestBase {

    private static final int _reasonableWaitTime = 3000;

    @Test
    public void canModifyMetadataInSubscriptionBatch() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            String sub = store.subscriptions().create(User.class);

            SubscriptionWorkerOptions workerOptions = new SubscriptionWorkerOptions(sub);
            workerOptions.setTimeToWaitBeforeConnectionRetry(Duration.ofSeconds(5));
            workerOptions.setMaxDocsPerBatch(2);

            SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, workerOptions);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 2; i++) {
                    User user = new User();
                    user.setCount(1);
                    user.setId("Users/" + i);
                    session.store(user);
                }

                session.saveChanges();
            }

            Semaphore docs = new Semaphore(0);

            String date1 = DateUtils.addHours(RavenTestHelper.utcToday(), 1).toString();
            String date2 = DateUtils.addHours(RavenTestHelper.utcToday(), 2).toString();

            subscription.run(x -> {
                try (IDocumentSession session = x.openSession()) {
                    for (SubscriptionBatch.Item<User> item : x.getItems()) {
                        IMetadataDictionary meta = session.advanced().getMetadataFor(item.getResult());
                        meta.put("Test1", date1);
                        item.getMetadata().put("Test2", date2);
                    }

                    session.saveChanges();

                    docs.release(x.getNumberOfItemsInBatch());
                }
            });

            assertThat(docs.tryAcquire(2, _reasonableWaitTime, TimeUnit.MILLISECONDS))
                    .isTrue();

            for (int i = 0; i < 2; i++) {
                try (IDocumentSession session = store.openSession()) {
                    User u = session.load(User.class, "Users/" + i);
                    IMetadataDictionary meta = session.advanced().getMetadataFor(u);
                    String metaDate1 = meta.getString("Test1");
                    String metaDate2 = meta.getString("Test2");

                    assertThat(metaDate1)
                            .isEqualTo(date1);
                    assertThat(metaDate2)
                            .isEqualTo(date2);
                }
            }
        }
    }
}
