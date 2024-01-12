package net.ravendb.client.test.client.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.GetOngoingTaskInfoOperation;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskSubscription;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.documents.operations.ongoingTasks.ToggleOngoingTaskStateOperation;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeType;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.documents.subscriptions.*;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriberErrorException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionClosedException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionDoesNotExistException;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionInUseException;
import net.ravendb.client.infrastructure.entities.Company;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.primitives.TimeValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("ConstantConditions")
public class SubscriptionsBasicTest extends RemoteTestBase {
    private final int _reasonableWaitTime = 60;

    @Test
    public void canDisableSubscriptionViaApi() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String subscription = store.subscriptions().create(User.class);

            store.subscriptions().disable(subscription);

            List<SubscriptionState> subscriptions = store.subscriptions().getSubscriptions(0, 10);
            assertThat(subscriptions.get(0).isDisabled())
                    .isTrue();

            store.subscriptions().enable(subscription);
            subscriptions = store.subscriptions().getSubscriptions(0, 10);
            assertThat(subscriptions.get(0).isDisabled())
                    .isFalse();
        }
    }

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
            IOUtils.closeQuietly(subscriptionWorker, null);
            IOUtils.closeQuietly(throwingSubscriptionWorker, null);
            IOUtils.closeQuietly(notThrowingSubscriptionWorker, null);
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
    @Disabled("RavenDB-15919, need to change the test, since we update the ChangeVectorForNextBatchStartingPoint upon fetching and not acking")
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

    @Test
    public void canUpdateSubscriptionByName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions subscriptionCreationOptions = new SubscriptionCreationOptions();
            subscriptionCreationOptions.setQuery("from Users");
            subscriptionCreationOptions.setName("Created");

            String subsId = store.subscriptions().create(subscriptionCreationOptions);

            List<SubscriptionState> subscriptions = store.subscriptions().getSubscriptions(0, 5);

            SubscriptionState state = subscriptions.get(0);

            assertThat(subscriptions)
                    .hasSize(1);
            assertThat(state.getSubscriptionName())
                    .isEqualTo("Created");
            assertThat(state.getQuery())
                    .isEqualTo("from Users");

            String newQuery = "from Users where age > 18";

            SubscriptionUpdateOptions subscriptionUpdateOptions = new SubscriptionUpdateOptions();
            subscriptionUpdateOptions.setName(subsId);
            subscriptionUpdateOptions.setQuery(newQuery);
            store.subscriptions().update(subscriptionUpdateOptions);

            List<SubscriptionState> newSubscriptions = store.subscriptions().getSubscriptions(0, 5);
            SubscriptionState newState = newSubscriptions.get(0);
            assertThat(newSubscriptions)
                    .hasSize(1);
            assertThat(newState.getSubscriptionName())
                    .isEqualTo(state.getSubscriptionName());
            assertThat(newState.getQuery())
                    .isEqualTo(newQuery);
            assertThat(newState.getSubscriptionId())
                    .isEqualTo(state.getSubscriptionId());
        }
    }

    @Test
    public void canUpdateSubscriptionById() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions subscriptionCreationOptions = new SubscriptionCreationOptions();
            subscriptionCreationOptions.setQuery("from Users");
            subscriptionCreationOptions.setName("Created");

            store.subscriptions().create(subscriptionCreationOptions);

            List<SubscriptionState> subscriptions = store.subscriptions().getSubscriptions(0, 5);

            SubscriptionState state = subscriptions.get(0);

            assertThat(subscriptions)
                    .hasSize(1);
            assertThat(state.getSubscriptionName())
                    .isEqualTo("Created");
            assertThat(state.getQuery())
                    .isEqualTo("from Users");

            String newQuery = "from Users where age > 18";

            SubscriptionUpdateOptions subscriptionUpdateOptions = new SubscriptionUpdateOptions();
            subscriptionUpdateOptions.setId(state.getSubscriptionId());
            subscriptionUpdateOptions.setQuery(newQuery);
            store.subscriptions().update(subscriptionUpdateOptions);

            List<SubscriptionState> newSubscriptions = store.subscriptions().getSubscriptions(0, 5);
            SubscriptionState newState = newSubscriptions.get(0);
            assertThat(newSubscriptions)
                    .hasSize(1);
            assertThat(newState.getSubscriptionName())
                    .isEqualTo(state.getSubscriptionName());
            assertThat(newState.getQuery())
                    .isEqualTo(newQuery);
            assertThat(newState.getSubscriptionId())
                    .isEqualTo(state.getSubscriptionId());
        }
    }

    @Test
    public void updateNonExistentSubscriptionShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = "Update";
            long id = 322;

            assertThatThrownBy(() -> {
                SubscriptionUpdateOptions subscriptionUpdateOptions = new SubscriptionUpdateOptions();
                subscriptionUpdateOptions.setName(name);
                store.subscriptions().update(subscriptionUpdateOptions);
            })
                    .isInstanceOf(SubscriptionDoesNotExistException.class);

            assertThatThrownBy(() -> {
                SubscriptionUpdateOptions subscriptionUpdateOptions = new SubscriptionUpdateOptions();
                subscriptionUpdateOptions.setName(name);
                subscriptionUpdateOptions.setId(id);
                store.subscriptions().update(subscriptionUpdateOptions);
            })
                    .isInstanceOf(SubscriptionDoesNotExistException.class);

            SubscriptionCreationOptions subscriptionCreationOptions = new SubscriptionCreationOptions();
            subscriptionCreationOptions.setQuery("from Users");
            subscriptionCreationOptions.setName("Created");
            String subsId = store.subscriptions().create(subscriptionCreationOptions);

            assertThatThrownBy(() -> {
                SubscriptionUpdateOptions subscriptionUpdateOptions = new SubscriptionUpdateOptions();
                subscriptionUpdateOptions.setName(subsId);
                subscriptionUpdateOptions.setId(id);
                store.subscriptions().update(subscriptionUpdateOptions);
            }).isInstanceOf(SubscriptionDoesNotExistException.class);
        }
    }

    @Test
    public void updateSubscriptionShouldReturnNotModified() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionUpdateOptions updateOptions = new SubscriptionUpdateOptions();
            updateOptions.setQuery("from Users");
            updateOptions.setName("Created");

            store.subscriptions().create(updateOptions);

            List<SubscriptionState> subscriptions = store.subscriptions().getSubscriptions(0, 5);

            SubscriptionState state = subscriptions.get(0);

            assertThat(subscriptions)
                    .hasSize(1);
            assertThat(state.getSubscriptionName())
                    .isEqualTo("Created");
            assertThat(state.getQuery())
                    .isEqualTo("from Users");

            store.subscriptions().update(updateOptions);

            List<SubscriptionState> newSubscriptions = store.subscriptions().getSubscriptions(0, 5);
            SubscriptionState newState = newSubscriptions.get(0);
            assertThat(newSubscriptions)
                    .hasSize(1);
            assertThat(newState.getSubscriptionName())
                    .isEqualTo(state.getSubscriptionName());
            assertThat(newState.getQuery())
                    .isEqualTo(state.getQuery());
            assertThat(newState.getSubscriptionId())
                    .isEqualTo(state.getSubscriptionId());
        }
    }



    @Test
    public void disposeSubscriptionWorkerShouldNotThrow() throws Exception {
        Semaphore mre = new Semaphore(0);
        Semaphore mre2 = new Semaphore(0);

        try (IDocumentStore store = getDocumentStore()) {
            store.getRequestExecutor().addOnBeforeRequestListener((sender, handler) -> {
                if (handler.getUrl().contains("info/remote-task/tcp?database=")) {
                    mre.release();
                    try {
                        assertThat(mre2.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                                .isTrue();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            String id = store.subscriptions().create(Company.class, new SubscriptionCreationOptions());
            SubscriptionWorkerOptions workerOptions = new SubscriptionWorkerOptions(id);
            workerOptions.setIgnoreSubscriberErrors(true);
            workerOptions.setStrategy(SubscriptionOpeningStrategy.TAKE_OVER);
            SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, workerOptions, store.getDatabase());

            CompletableFuture<Void> t = worker.run(x -> {
            });

            assertThat(mre.tryAcquire(_reasonableWaitTime, TimeUnit.SECONDS))
                    .isTrue();
            worker.close(false);
            mre2.release();

            Thread.sleep(5000);
            waitForValue(() -> t.isDone(), true, Duration.ofSeconds(5));
            assertThat(t.isCompletedExceptionally())
                    .isFalse();
        }
    }

    @Test
    public void canCreateSubscriptionWithIncludeTimeSeries_LastRangeByTime() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions subscriptionCreationOptions = new SubscriptionCreationOptions();
            subscriptionCreationOptions.setIncludes(b -> b.includeTimeSeries("stockPrice", TimeSeriesRangeType.LAST, TimeValue.ofMonths(1)));

            String name = store.subscriptions()
                    .create(Company.class, subscriptionCreationOptions);

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name)) {
                Semaphore mre = new Semaphore(0);

                worker.run(batch -> {
                    try (IDocumentSession session = batch.openSession()) {
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        Company company = session.load(Company.class, "companies/1");
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        ISessionDocumentTimeSeries timeSeries = session.timeSeriesFor(company, "stockPrice");
                        TimeSeriesEntry[] timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -7), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(now);
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(10);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isEqualTo(0);
                    }

                    mre.release();
                });

                try (IDocumentSession session = store.openSession()) {
                    Company company = new Company();
                    company.setId("companies/1");
                    company.setName("HR");

                    session.store(company);

                    session.timeSeriesFor(company, "stockPrice")
                            .append(now, 10);

                    session.saveChanges();
                }

                assertThat(mre.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    public void canCreateSubscriptionWithIncludeTimeSeries_LastRangeByCount() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setIncludes(b -> b.includeTimeSeries("stockPrice", TimeSeriesRangeType.LAST, 32));
            String name = store.subscriptions()
                    .create(Company.class, creationOptions);

            Semaphore mre = new Semaphore(0);

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name)) {
                worker.run(batch -> {
                    try (IDocumentSession session = batch.openSession()) {
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        Company company = session.load(Company.class, "companies/1");
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        ISessionDocumentTimeSeries timeSeries = session.timeSeriesFor(company, "stockPrice");
                        TimeSeriesEntry[] timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -7), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(DateUtils.addDays(now, -7));
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(10);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();
                    }

                    mre.release();
                });

                try (IDocumentSession session = store.openSession()) {
                    Company company = new Company();
                    company.setId("companies/1");
                    company.setName("HR");

                    session.store(company);

                    session.timeSeriesFor(company, "stockPrice")
                            .append(DateUtils.addDays(now, -7), 10);
                    session.saveChanges();
                }

                assertThat(mre.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    public void canCreateSubscriptionWithIncludeTimeSeries_Array_LastRange() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setIncludes(builder
                    -> builder.includeTimeSeries(
                            new String[] { "stockPrice", "stockPrice2" }, TimeSeriesRangeType.LAST, TimeValue.ofDays(7)));

            String name = store.subscriptions().create(Company.class, creationOptions);

            Semaphore mre = new Semaphore(0);

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name)) {
                worker.run(batch -> {
                    try (IDocumentSession session = batch.openSession()) {
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        Company company = session.load(Company.class, "companies/1");
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        ISessionDocumentTimeSeries timeSeries = session.timeSeriesFor(company, "stockPrice");
                        TimeSeriesEntry[] timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -7), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(DateUtils.addDays(now, -7));
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(10);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isEqualTo(0);

                        timeSeries = session.timeSeriesFor(company, "stockPrice2");
                        timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -5), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(DateUtils.addDays(now, -5));
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(100);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isEqualTo(0);
                    }

                    mre.release();
                });

                try (IDocumentSession session = store.openSession()) {
                    Company company = new Company();
                    company.setId("companies/1");
                    company.setName("HR");

                    session.store(company);

                    session.timeSeriesFor(company, "stockPrice")
                            .append(DateUtils.addDays(now, -7), 10);
                    session.timeSeriesFor(company, "stockPrice2")
                            .append(DateUtils.addDays(now, -5), 100);

                    session.saveChanges();
                }

                assertThat(mre.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    public void canCreateSubscriptionWithIncludeTimeSeries_All_LastRange() throws Exception {
        Date now = RavenTestHelper.utcToday();

        try (IDocumentStore store = getDocumentStore()) {
            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setIncludes(builder -> builder.includeAllTimeSeries(TimeSeriesRangeType.LAST, TimeValue.ofDays(7)));

            String name = store.subscriptions().create(Company.class, creationOptions);

            Semaphore mre = new Semaphore(0);

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name)) {
                worker.run(batch -> {
                    try (IDocumentSession session = batch.openSession()) {
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        Company company = session.load(Company.class, "companies/1");
                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        ISessionDocumentTimeSeries timeSeries = session.timeSeriesFor(company, "stockPrice");
                        TimeSeriesEntry[] timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -7), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(DateUtils.addDays(now, -7));
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(10);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();

                        timeSeries = session.timeSeriesFor(company, "stockPrice2");
                        timeSeriesEntries = timeSeries.get(DateUtils.addDays(now, -5), null);

                        assertThat(timeSeriesEntries)
                                .hasSize(1);
                        assertThat(timeSeriesEntries[0].getTimestamp())
                                .isEqualTo(DateUtils.addDays(now, -5));
                        assertThat(timeSeriesEntries[0].getValue())
                                .isEqualTo(100);

                        assertThat(session.advanced().getNumberOfRequests())
                                .isZero();
                    }

                    mre.release();
                });

                try (IDocumentSession session = store.openSession()) {
                    Company company = new Company();
                    company.setId("companies/1");
                    company.setName("HR");

                    session.store(company);

                    session.timeSeriesFor(company, "stockPrice")
                            .append(DateUtils.addDays(now, -7), 10);
                    session.timeSeriesFor(company, "stockPrice2")
                            .append(DateUtils.addDays(now, -5), 100);

                    session.saveChanges();
                }

                assertThat(mre.tryAcquire(30, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    @Test
    public void canUseEmoji() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1;
            try (IDocumentSession session = store.openSession()) {

                user1 = new User();
                user1.setName("user_\uD83D\uDE21\uD83D\uDE21\uD83E\uDD2C\uD83D\uDE00😡😡🤬😀");
                session.store(user1, "users/1");

                session.saveChanges();
            }

            SubscriptionCreationOptions creationOptions = new SubscriptionCreationOptions();
            creationOptions.setName("name_\uD83D\uDE21\uD83D\uDE21\uD83E\uDD2C\uD83D\uDE00😡😡🤬😀");
            String id = store.subscriptions().create(User.class, creationOptions);

            try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, new SubscriptionWorkerOptions(id))) {
                BlockingArrayQueue<String> keys = new BlockingArrayQueue<>();
                subscription.run(batch -> batch.getItems().forEach(x -> keys.add(x.getResult().getName())));

                String key = keys.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                assertThat(key)
                        .isNotNull()
                        .isEqualTo(user1.getName());

            }
        }
    }
}
