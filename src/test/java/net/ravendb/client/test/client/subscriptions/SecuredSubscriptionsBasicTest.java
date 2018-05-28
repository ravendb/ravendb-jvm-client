package net.ravendb.client.test.client.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.entities.User;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SecuredSubscriptionsBasicTest extends RemoteTestBase {
    private final int _reasonableWaitTime = 5;


    @Test
    public void shouldStreamAllDocumentsAfterSubscriptionCreation() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setAge(31);
                session.store(user1, "users/1");

                User user2 = new User();
                user2.setAge(27);
                session.store(user2, "users/12");

                User user3 = new User();
                user3.setAge(25);
                session.store(user3, "users/3");

                session.saveChanges();
            }

            String id = store.subscriptions().create(User.class);

            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, new SubscriptionWorkerOptions(id))) {

                BlockingArrayQueue<String> keys = new BlockingArrayQueue<>();
                BlockingArrayQueue<Integer> ages = new BlockingArrayQueue<>();
                subscription.run(batch -> {
                    batch.getItems().forEach(x -> keys.add(x.getId()));
                    batch.getItems().forEach(x -> ages.add(x.getResult().getAge()));
                });

                String key = keys.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(key)
                        .isNotNull()
                        .isEqualTo("users/1");

                key = keys.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(key)
                        .isNotNull()
                        .isEqualTo("users/12");

                key = keys.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(key)
                        .isNotNull()
                        .isEqualTo("users/3");

                int age = ages.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(age)
                        .isEqualTo(31);

                age = ages.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(age)
                        .isEqualTo(27);

                age = ages.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(age)
                        .isEqualTo(25);
            }
        }
    }

    @Test
    public void shouldSendAllNewAndModifiedDocs() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {
            String id = store.subscriptions().create(User.class);

            try (SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(id))) {
                ArrayBlockingQueue<String> names = new ArrayBlockingQueue<>(20);

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("James");
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                subscription.run(batch -> batch.getItems().forEach(x -> names.add(x.getResult().get("name").asText())));

                String name = names.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(name)
                        .isEqualTo("James");

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("Adam");
                    session.store(user, "users/12");
                    session.saveChanges();
                }

                name = names.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(name)
                        .isEqualTo("Adam");

                //Thread.sleep(15000); // test with sleep - let few heartbeats come to us - commented out for CI

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("David");
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                name = names.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(name)
                        .isEqualTo("David");
            }
        }
    }
}
