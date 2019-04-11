package net.ravendb.client.test.client.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskSubscription;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.operations.ongoingTasks.ToggleOngoingTaskStateOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.*;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriberErrorException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionClosedException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionDoesNotExistException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionInUseException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.ExceptionsUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("ConstantConditions")
public class SubscriptionsBasicTest extends RemoteTestBase {
    private final int _reasonableWaitTime = 60;

    @Test
    public void canDeleteSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id1 = store.subscriptions().create(User.class);
            String id2 = store.subscriptions().create(User.class);

            List<SubscriptionState> subscriptions =
                    store.subscriptions().getSubscriptions(0, 5);

            assertThat(subscriptions)
                    .hasSize(2);

            // test getSubscriptionState as well
            SubscriptionState subscriptionState = store.subscriptions().getSubscriptionState(id1);
            assertThat(subscriptionState.getChangeVectorForNextBatchStartingPoint())
                    .isNull();


            store.subscriptions().delete(id1);
            store.subscriptions().delete(id2);

            subscriptions = store.subscriptions().getSubscriptions(0, 5);

            assertThat(subscriptions)
                    .isEmpty();
        }
    }

    @Test
    public void shouldThrowWhenOpeningNoExistingSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions("1"));
            assertThatThrownBy(() -> subscription.run(x -> {}).get())
                    .matches(ex -> {
                        RuntimeException e = ExceptionsUtils.unwrapException(ex);

                        return e instanceof SubscriptionDoesNotExistException;
                    }, "subclass of " + SubscriptionDoesNotExistException.class.getName());
        }
    }

    @Test
    public void shouldThrowOnAttemptToOpenAlreadyOpenedSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class);
            try (SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(id))) {
                try (IDocumentSession session = store.openSession()) {
                    session.store(new User());
                    session.saveChanges();
                }

                Semaphore semaphore = new Semaphore(0);
                CompletableFuture<Void> t = subscription.run(x -> semaphore.release());

                semaphore.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS);


                SubscriptionWorkerOptions options2 = new SubscriptionWorkerOptions(id);
                options2.setStrategy(SubscriptionOpeningStrategy.OPEN_IF_FREE);
                try (SubscriptionWorker<ObjectNode> secondSubscription = store.subscriptions().getSubscriptionWorker(options2)) {
                    assertThatThrownBy(() -> secondSubscription.run(x -> {}).get())
                            .matches(ex -> {
                                RuntimeException e = ExceptionsUtils.unwrapException(ex);
                                return e instanceof SubscriptionInUseException;
                            }, " expected subscription in use");
                }
            }
        }
    }

    @Test
    public void shouldStreamAllDocumentsAfterSubscriptionCreation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
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
        try (IDocumentStore store = getDocumentStore()) {
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


    @Test
    public void shouldRespectMaxDocCountInBatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    session.store(new Company());
                }

                session.saveChanges();
            }

            String id = store.subscriptions().create(Company.class);
            SubscriptionWorkerOptions options = new SubscriptionWorkerOptions(id);
            options.setMaxDocsPerBatch(25);
            try (SubscriptionWorker<ObjectNode> subscriptionWorker = store.subscriptions().getSubscriptionWorker(options)) {

                Semaphore semaphore = new Semaphore(0);

                AtomicInteger totalItems = new AtomicInteger();

                subscriptionWorker.run(batch -> {
                    totalItems.addAndGet(batch.getNumberOfItemsInBatch());
                    assertThat(batch.getNumberOfItemsInBatch())
                            .isLessThanOrEqualTo(25);

                    if (totalItems.get() == 100) {
                        semaphore.release();
                    }
                });

                assertThat(semaphore.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    public void shouldRespectCollectionCriteria() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 100; i++) {
                    session.store(new Company());
                    session.store(new User());
                }

                session.saveChanges();
            }

            String id = store.subscriptions().create(User.class);

            SubscriptionWorkerOptions options = new SubscriptionWorkerOptions(id);
            options.setMaxDocsPerBatch(31);
            try (SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(options)) {
                Semaphore semaphore = new Semaphore(0);
                AtomicInteger atomicInteger = new AtomicInteger();

                subscription.run(batch -> {
                    atomicInteger.addAndGet(batch.getNumberOfItemsInBatch());

                    if (atomicInteger.get() == 100) {
                        semaphore.release();
                    }
                });

                assertThat(semaphore.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    @Disabled("waiting for: RavenDB-13380")
    public void canDisableSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 10; i++) {
                    session.store(new Company());
                    session.store(new User());
                }

                session.saveChanges();
            }

            String id = store.subscriptions().create(User.class);

            OngoingTaskSubscription subscriptionTask
                    = (OngoingTaskSubscription) store.maintenance().send(new GetOngoingTaskInfoOperation(id, OngoingTaskType.SUBSCRIPTION));
            assertThat(subscriptionTask)
                    .isNotNull();

            store.maintenance().send(
                    new ToggleOngoingTaskStateOperation(subscriptionTask.getTaskId(), OngoingTaskType.SUBSCRIPTION, true));

            subscriptionTask
                    = (OngoingTaskSubscription) store.maintenance().send(new GetOngoingTaskInfoOperation(id, OngoingTaskType.SUBSCRIPTION));
            assertThat(subscriptionTask)
                    .isNotNull();
            assertThat(subscriptionTask.isDisabled())
                    .isTrue();
        }
    }

    @Test
    public void willAcknowledgeEmptyBatches() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            List<SubscriptionState> subscriptionDocuments = store.subscriptions().getSubscriptions(0, 10);

            assertThat(subscriptionDocuments)
                    .isEmpty();

            String allId = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            try (SubscriptionWorker<ObjectNode> allSubscription = store.subscriptions().getSubscriptionWorker(allId)) {
                Semaphore allSemaphore = new Semaphore(0);
                AtomicInteger allCounter = new AtomicInteger();

                SubscriptionCreationOptions filteredOptions = new SubscriptionCreationOptions();
                filteredOptions.setQuery("from Users where age < 0");
                String filteredUsersId = store.subscriptions().create(filteredOptions);

                try (SubscriptionWorker<ObjectNode> filteredUsersSubscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(filteredUsersId))) {
                    Semaphore usersDocsSemaphore = new Semaphore(0);

                    try (IDocumentSession session = store.openSession()) {
                        for (int i = 0; i < 500; i++) {
                            session.store(new User(), "another/");
                        }
                        session.saveChanges();
                    }

                    allSubscription.run(x -> {
                        allCounter.addAndGet(x.getNumberOfItemsInBatch());

                        if (allCounter.get() >= 100) {
                            allSemaphore.release();
                        }
                    });

                    filteredUsersSubscription.run(x -> usersDocsSemaphore.release());

                    assertThat(allSemaphore.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                            .isTrue();
                    assertThat(usersDocsSemaphore.tryAcquire(50, TimeUnit.MILLISECONDS))
                            .isFalse();
                }

            }
        }
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    public void canReleaseSubscription() throws Exception {
        SubscriptionWorker<ObjectNode> subscriptionWorker = null;
        SubscriptionWorker<ObjectNode> throwingSubscriptionWorker = null;
        SubscriptionWorker<ObjectNode> notThrowingSubscriptionWorker = null;

        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            SubscriptionWorkerOptions options1 = new SubscriptionWorkerOptions(id);
            options1.setStrategy(SubscriptionOpeningStrategy.OPEN_IF_FREE);
            subscriptionWorker = store.subscriptions().getSubscriptionWorker(options1);

            Semaphore mre = new Semaphore(0);
            putUserDoc(store);

            CompletableFuture<Void> t = subscriptionWorker.run(x -> mre.release());
            assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                    .isTrue();

            SubscriptionWorkerOptions options2 = new SubscriptionWorkerOptions(id);
            options2.setStrategy(SubscriptionOpeningStrategy.OPEN_IF_FREE);
            throwingSubscriptionWorker = store.subscriptions().getSubscriptionWorker(options2);

            CompletableFuture<Void> subscriptionTask = throwingSubscriptionWorker.run(x -> { });

            assertThatThrownBy(() -> subscriptionTask.get()).matches(x -> {
                RuntimeException exception = ExceptionsUtils.unwrapException(x);
                return exception instanceof SubscriptionInUseException;
            }, "expected SubscriptionInUseException");

            store.subscriptions().dropConnection(id);

            notThrowingSubscriptionWorker = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(id));

            t = notThrowingSubscriptionWorker.run(x -> mre.release());

            putUserDoc(store);

            assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                    .isTrue();
        } finally {
            IOUtils.closeQuietly(subscriptionWorker);
            IOUtils.closeQuietly(throwingSubscriptionWorker);
            IOUtils.closeQuietly(notThrowingSubscriptionWorker);
        }
    }

    private static void putUserDoc(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            session.store(new User());
            session.saveChanges();
        }
    }

    @Test
    public void shouldPullDocumentsAfterBulkInsert() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class, new SubscriptionCreationOptions());

            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, new SubscriptionWorkerOptions(id))) {
                BlockingQueue<User> docs = new ArrayBlockingQueue<>(10);

                try (BulkInsertOperation bulk = store.bulkInsert()) {
                    bulk.store(new User());
                    bulk.store(new User());
                    bulk.store(new User());
                }

                subscription.run(x -> x.getItems().forEach(i -> docs.add(i.getResult())));

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();
            }
        }
    }

    @Test
    public void shouldStopPullingDocsAndCloseSubscriptionOnSubscriberErrorByDefault() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            try (SubscriptionWorker<ObjectNode> subscription = store.subscriptions().getSubscriptionWorker(new SubscriptionWorkerOptions(id))) {
                putUserDoc(store);

                CompletableFuture<Void> subscriptionTask = subscription.run(x -> {
                    throw new RuntimeException("Fake exception");
                });

                assertThatThrownBy(()-> subscriptionTask.get(_reasonableWaitTime, TimeUnit.SECONDS))
                        .matches(x -> {
                            RuntimeException exception = ExceptionsUtils.unwrapException(x);
                            return exception instanceof SubscriberErrorException;
                        });

                SubscriptionState subscriptionConfig = store.subscriptions().getSubscriptions(0, 1).get(0);
                assertThat(subscriptionConfig.getChangeVectorForNextBatchStartingPoint())
                        .isNullOrEmpty();
            }
        }
    }

    @Test
    public void canSetToIgnoreSubscriberErrors() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            SubscriptionWorkerOptions options1 = new SubscriptionWorkerOptions(id);
            options1.setIgnoreSubscriberErrors(true);
            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, options1)) {
                ArrayBlockingQueue<User> docs = new ArrayBlockingQueue<>(20);

                putUserDoc(store);
                putUserDoc(store);

                CompletableFuture<Void> subscriptionTask = subscription.run(x -> {
                    x.getItems().forEach(i -> docs.add(i.getResult()));
                    throw new RuntimeException("Fake exception");
                });

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();

                assertThat(subscriptionTask.isCompletedExceptionally())
                        .isFalse();
            }
        }
    }

    @Test
    public void ravenDB_3452_ShouldStopPullingDocsIfReleased() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class);

            SubscriptionWorkerOptions options1 = new SubscriptionWorkerOptions(id);
            options1.setTimeToWaitBeforeConnectionRetry(Duration.ofSeconds(1));
            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, options1)) {
                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.store(new User(), "users/2");
                    session.saveChanges();
                }

                ArrayBlockingQueue<User> docs = new ArrayBlockingQueue<>(20);
                CompletableFuture<Void> subscribe = subscription.run(x -> x.getItems().forEach(i -> docs.add(i.getResult())));

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();

                assertThat(docs.poll(_reasonableWaitTime, TimeUnit.SECONDS))
                        .isNotNull();

                store.subscriptions().dropConnection(id);

                try {
                    // this can exit normally or throw on drop connection
                    // depending on exactly where the drop happens
                    subscribe.get(_reasonableWaitTime, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    assertThat(ExceptionsUtils.unwrapException(e))
                            .isExactlyInstanceOf(SubscriptionClosedException.class);
                }

                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/3");
                    session.store(new User(), "users/4");
                    session.saveChanges();
                }

                assertThat(docs.poll(50, TimeUnit.MILLISECONDS))
                        .isNull();

                assertThat(docs.poll(50, TimeUnit.MILLISECONDS))
                        .isNull();

                assertThat(subscribe.isDone())
                        .isTrue();
            }
        }
    }

    @Test
    public void ravenDB_3453_ShouldDeserializeTheWholeDocumentsAfterTypedSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, id)) {
                ArrayBlockingQueue<User> users = new ArrayBlockingQueue<>(20);

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

                subscription.run(x -> x.getItems().forEach(i -> users.add(i.getResult())));

                User user;
                user = users.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(user)
                        .isNotNull();
                assertThat(user.getId())
                        .isEqualTo("users/1");
                assertThat(user.getAge())
                        .isEqualTo(31);

                user = users.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(user)
                        .isNotNull();
                assertThat(user.getId())
                        .isEqualTo("users/12");
                assertThat(user.getAge())
                        .isEqualTo(27);

                user = users.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(user)
                        .isNotNull();
                assertThat(user.getId())
                        .isEqualTo("users/3");
                assertThat(user.getAge())
                        .isEqualTo(25);

            }
        }
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Test
    public void disposingOneSubscriptionShouldNotAffectOnNotificationsOfOthers() throws Exception {
        SubscriptionWorker<User> subscription1 = null;
        SubscriptionWorker<User> subscription2 = null;

        try (IDocumentStore store = getDocumentStore()) {
            String id1 = store.subscriptions().create(User.class, new SubscriptionCreationOptions());
            String id2 = store.subscriptions().create(User.class, new SubscriptionCreationOptions());

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");
                session.store(new User(), "users/2");
                session.saveChanges();
            }

            subscription1 = store.subscriptions().getSubscriptionWorker(User.class, id1);
            ArrayBlockingQueue<User> items1 = new ArrayBlockingQueue<>(10);
            subscription1.run(x -> x.getItems().forEach(i -> items1.add(i.getResult())));

            subscription2 = store.subscriptions().getSubscriptionWorker(User.class, id2);
            ArrayBlockingQueue<User> items2 = new ArrayBlockingQueue<>(10);
            subscription2.run(x -> x.getItems().forEach(i -> items2.add(i.getResult())));

            User user = items1.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();
            assertThat(user.getId())
                    .isEqualTo("users/1");

            user = items1.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();

            assertThat(user.getId())
                    .isEqualTo("users/2");

            user = items2.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();
            assertThat(user.getId())
                    .isEqualTo("users/1");

            user = items2.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();

            assertThat(user.getId())
                    .isEqualTo("users/2");

            subscription1.close();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/3");
                session.store(new User(), "users/4");
                session.saveChanges();
            }

            user = items2.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();
            assertThat(user.getId())
                    .isEqualTo("users/3");

            user = items2.poll(_reasonableWaitTime, TimeUnit.SECONDS);
            assertThat(user)
                    .isNotNull();

            assertThat(user.getId())
                    .isEqualTo("users/4");
        } finally {
            subscription1.close();
            subscription2.close();
        }
    }
}
