package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.Revision;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RavenDB_13762Test extends RemoteTestBase {

    private final int _reasonableWaitTime = 15;

    @Test
    public void sessionInSubscriptionsShouldNotTrackRevisions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String subscriptionId = store.subscriptions().createForRevisions(User.class);

            RevisionsConfiguration configuration = new RevisionsConfiguration();

            RevisionsCollectionConfiguration defaultConfiguration = new RevisionsCollectionConfiguration();
            defaultConfiguration.setDisabled(false);
            defaultConfiguration.setMinimumRevisionsToKeep(5L);

            configuration.setDefaultConfig(defaultConfiguration);

            RevisionsCollectionConfiguration userConfiguration = new RevisionsCollectionConfiguration();
            userConfiguration.setDisabled(false);

            configuration.setCollections(Collections.singletonMap("Users", userConfiguration));

            ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(configuration);
            store.maintenance().send(operation);

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    try (IDocumentSession session = store.openSession()) {
                        User user = new User();
                        user.setName("users" + i + " ver " + j);
                        session.store(user, "users/" + i);

                        session.saveChanges();
                    }
                }
            }

            try (SubscriptionWorker<Revision<User>> sub = store.subscriptions().getSubscriptionWorkerForRevisions(User.class, new SubscriptionWorkerOptions(subscriptionId))) {
                Semaphore mre = new Semaphore(0);

                AtomicReference<Exception> exception = new AtomicReference<>();

                sub.run(x -> {
                    try (IDocumentSession session = x.openSession()) {
                        x.getItems().get(0).getResult().getCurrent().setName("aaaa");

                        session.saveChanges();
                    } catch (Exception e) {
                        exception.set(e);
                    } finally {
                        mre.release();
                    }

                });

                assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();

                if (exception.get() != null) {
                    throw new RuntimeException(exception.get());
                }
            }
        }
    }
}
