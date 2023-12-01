package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.Revision;
import net.ravendb.client.documents.subscriptions.SubscriptionBatch;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RavenDB_11166Test extends RemoteTestBase {

    private final int _reasonableWaitTime = 15;

    public static class Dog {
        private String name;
        private String owner;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }
    }

    public static class Person {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void canUseSubscriptionWithIncludes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("Arava");
                session.store(person, "people/1");

                Dog dog = new Dog();
                dog.setName("Oscar");
                dog.setOwner("people/1");
                session.store(dog);

                session.saveChanges();
            }

            SubscriptionCreationOptions options = new SubscriptionCreationOptions();
            options.setQuery("from Dogs include owner");
            String id = store.subscriptions().create(options);

            try (SubscriptionWorker<Dog> sub = store.subscriptions().getSubscriptionWorker(Dog.class, id)) {

                Semaphore semaphore = new Semaphore(0);
                CompletableFuture<Void> run = sub.run(batch -> {
                    assertThat(batch.getItems())
                            .isNotEmpty();

                    try (IDocumentSession s = batch.openSession()) {
                        for (SubscriptionBatch.Item<Dog> item : batch.getItems()) {
                            s.load(Person.class, item.getResult().getOwner());
                            Dog dog = s.load(Dog.class, item.getId());
                            assertThat(dog)
                                    .isSameAs(item.getResult());
                        }

                        assertThat(s.advanced().getNumberOfRequests())
                                .isZero();

                        semaphore.release();
                    }
                });

                assertThat(semaphore.tryAcquire(15, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }
    @Test
    public void canUseSubscriptionRevisionsWithIncludes() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            setupRevisions(store, false, 5);

            try (IDocumentSession session = store.openSession()) {
                Person person = new Person();
                person.setName("Arava");
                session.store(person, "people/1");

                Person person2 = new Person();
                person2.setName("Karmel");
                session.store(person2, "people/2");

                Dog dog = new Dog();
                dog.setName("Oscar");
                dog.setOwner("people/1");
                session.store(dog, "dogs/1");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Dog dog = new Dog();
                dog.setName("Oscar");
                dog.setOwner("people/2");
                session.store(dog, "dogs/1");
                session.saveChanges();
            }

            SubscriptionCreationOptions options = new SubscriptionCreationOptions();
            options.setQuery("from Dogs (Revisions = true) as d include d.Current.owner, d.Previous.owner");
            String id = store.subscriptions().create(options);

            try (SubscriptionWorker<Revision<Dog>> sub = store.subscriptions().getSubscriptionWorkerForRevisions(Dog.class, id)) {
                Semaphore mre = new Semaphore(0);


                sub.run(batch -> {
                    assertThat(batch.getItems())
                            .isNotEmpty();

                    try (IDocumentSession s = batch.openSession()) {

                        for (SubscriptionBatch.Item<Revision<Dog>> item : batch.getItems()) {
                            if (item.getResult().getPrevious() == null) {
                                continue;
                            }

                            Person currentOwner = s.load(Person.class, item.getResult().getCurrent().getOwner());
                            assertThat(currentOwner.getName())
                                    .isEqualTo("Karmel");
                            Person previousOwner = s.load(Person.class, item.getResult().getPrevious().getOwner());
                            assertThat(previousOwner.getName())
                                    .isEqualTo("Arava");
                        }

                        assertThat(s.advanced().getNumberOfRequests())
                                .isZero();
                    }

                    mre.release();
                });

                assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }
}
