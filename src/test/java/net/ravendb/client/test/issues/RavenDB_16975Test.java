package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16975Test extends RemoteTestBase {

    @Test
    public void should_Not_Send_Include_Message() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User person = new User();
                person.setName("Arava");
                session.store(person, "users/1");
                session.saveChanges();
            }

            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setQuery("from Users");
            String id = store.subscriptions().create(creationOptions);

            try (SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(id))) {
                Semaphore semaphore = new Semaphore(0);

                subscription.run(batch -> {
                    assertThat(batch.getItems())
                            .isNotEmpty();

                    try (IDocumentSession s = batch.openSession()) {
                        assertThat(batch.getNumberOfIncludes())
                                .isZero();
                        assertThat(s.advanced().getNumberOfRequests())
                                .isZero();
                    }

                    semaphore.release();
                });

                assertThat(semaphore.tryAcquire(10, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }
}
