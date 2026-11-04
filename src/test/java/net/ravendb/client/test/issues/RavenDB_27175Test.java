package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.ToggleDatabasesStateOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionUpdateOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerState;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerStatus;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionDoesNotExistException;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * The C# test breaks the connection through server internals. Here the server is made to drop it through the
 * public API instead: updating the subscription query closes live connections with canReconnect = true, and
 * disabling the database keeps every reconnect attempt failing until it is enabled again.
 */
public class RavenDB_27175Test extends RemoteTestBase {

    private static final long WAIT_SECONDS = 60;

    private static void dropConnectionWithReconnect(IDocumentStore store, String name) {
        SubscriptionUpdateOptions options = new SubscriptionUpdateOptions();
        options.setName(name);
        options.setQuery("from Companies where Name != 'no-such-name-" + System.nanoTime() + "'");
        store.subscriptions().update(options);
    }

    private static SubscriptionWorkerStatus nextStatus(BlockingQueue<SubscriptionWorkerStatus> changes, SubscriptionWorkerState state) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (true) {
            SubscriptionWorkerStatus status = changes.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (status == null) {
                fail("Did not reach state " + state + " in time");
            }
            if (status.getState() == state) {
                return status;
            }
        }
    }

    @Test
    public void canBeNotifiedOnReconnectionOfAnIdleSubscription() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = store.subscriptions().create(Company.class);

            SubscriptionWorkerOptions options = new SubscriptionWorkerOptions(name);
            options.setTimeToWaitBeforeConnectionRetry(Duration.ofMillis(16));

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, options)) {
                CountDownLatch firstConnection = new CountDownLatch(1);
                CountDownLatch reconnection = new CountDownLatch(1);
                CountDownLatch retry = new CountDownLatch(1);
                AtomicInteger connections = new AtomicInteger();

                worker.addOnEstablishedSubscriptionConnection(v -> {
                    if (connections.incrementAndGet() == 1) {
                        firstConnection.countDown();
                    } else {
                        reconnection.countDown();
                    }
                });
                worker.addOnSubscriptionConnectionRetry(e -> retry.countDown());

                // no documents are ever stored here, so the events are the only indication that we are connected
                worker.run(x -> { });

                assertThat(firstConnection.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("initial connection").isTrue();

                dropConnectionWithReconnect(store, name);

                assertThat(retry.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("onSubscriptionConnectionRetry").isTrue();
                assertThat(reconnection.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("onEstablishedSubscriptionConnection on reconnection").isTrue();
            }
        }
    }

    @Test
    public void statusFollowsTheWorkerFromConnectingThroughProcessing() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("RavenDB");
                session.store(company);
                session.saveChanges();
            }

            String name = store.subscriptions().create(Company.class);

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name)) {
                assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.NOT_STARTED);
                assertThat(worker.getStatus().getException()).isNull();

                ConcurrentLinkedQueue<SubscriptionWorkerState> observed = new ConcurrentLinkedQueue<>();
                ConcurrentLinkedQueue<Object> raisedBy = new ConcurrentLinkedQueue<>();

                worker.addOnStateChanged((sender, status) -> {
                    raisedBy.add(sender);
                    observed.add(status.getState());
                });

                AtomicReference<SubscriptionWorkerState> stateWhileProcessing = new AtomicReference<>(SubscriptionWorkerState.NOT_STARTED);
                CountDownLatch processed = new CountDownLatch(1);

                worker.run(batch -> {
                    stateWhileProcessing.set(worker.getStatus().getState());
                    processed.countDown();
                });

                assertThat(processed.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("the batch was handed to the subscriber").isTrue();

                assertThat(stateWhileProcessing.get()).isEqualTo(SubscriptionWorkerState.PROCESSING);

                // once the subscriber returned and the batch was acknowledged, the worker is idle again
                assertThat(waitForValue(() -> worker.getStatus().getState(), SubscriptionWorkerState.WAITING_FOR_DOCUMENTS, Duration.ofSeconds(WAIT_SECONDS)))
                        .isEqualTo(SubscriptionWorkerState.WAITING_FOR_DOCUMENTS);

                List<SubscriptionWorkerState> firstThree = new ArrayList<>(observed).subList(0, 3);
                assertThat(firstThree).containsExactly(
                        SubscriptionWorkerState.CONNECTING,
                        SubscriptionWorkerState.WAITING_FOR_DOCUMENTS,
                        SubscriptionWorkerState.PROCESSING);

                assertThat(raisedBy).isNotEmpty().allMatch(sender -> sender == worker);
            }
        }
    }

    @Test
    public void statusReportsRetryingWithTheFailureThatCausedIt() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = store.subscriptions().create(Company.class);

            SubscriptionWorkerOptions options = new SubscriptionWorkerOptions(name);
            // long enough that RETRYING is still the state while the worker waits out the delay
            options.setTimeToWaitBeforeConnectionRetry(Duration.ofSeconds(1));

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, options)) {
                CountDownLatch connected = new CountDownLatch(1);
                CompletableFuture<SubscriptionWorkerStatus> retrying = new CompletableFuture<>();

                worker.addOnStateChanged((sender, status) -> {
                    if (status.getState() == SubscriptionWorkerState.WAITING_FOR_DOCUMENTS) {
                        connected.countDown();
                    } else if (status.getState() == SubscriptionWorkerState.RETRYING) {
                        retrying.complete(status);
                    }
                });

                // no documents are ever stored here, so the state is the only indication of what the worker is doing
                worker.run(x -> { });

                assertThat(connected.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("initial connection").isTrue();
                assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.WAITING_FOR_DOCUMENTS);
                assertThat(worker.getStatus().getException()).isNull();

                dropConnectionWithReconnect(store, name);

                SubscriptionWorkerStatus status = retrying.get(WAIT_SECONDS, TimeUnit.SECONDS);

                assertThat(status.getException()).isNotNull();
                assertThat(status.getException().getMessage()).contains("query has been modified");

                // and the worker recovers on its own, clearing the failure
                assertThat(waitForValue(() -> worker.getStatus().getState(), SubscriptionWorkerState.WAITING_FOR_DOCUMENTS, Duration.ofSeconds(WAIT_SECONDS)))
                        .isEqualTo(SubscriptionWorkerState.WAITING_FOR_DOCUMENTS);

                assertThat(worker.getStatus().getException()).isNull();
            }
        }
    }

    @Test
    public void statusIsFaultedWithTheFailureWhenTheWorkerGivesUp() throws Exception {
        try (IDocumentStore store = getDocumentStore();
             SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, new SubscriptionWorkerOptions("no-such-subscription"))) {

            Throwable cause = null;
            try {
                worker.run(x -> { }).get(WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                cause = e.getCause();
            }

            assertThat(cause).isInstanceOf(SubscriptionDoesNotExistException.class);

            assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.FAULTED);
            assertThat(worker.getStatus().getException()).isSameAs(cause);
        }
    }

    @Test
    public void statusIsStoppedAfterClose() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = store.subscriptions().create(Company.class);

            SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name);
            CountDownLatch connected = new CountDownLatch(1);
            worker.addOnEstablishedSubscriptionConnection(v -> connected.countDown());

            worker.run(x -> { });

            assertThat(connected.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("initial connection").isTrue();

            worker.close();

            assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.STOPPED);
            assertThat(worker.getStatus().getException()).isNull();
        }
    }

    @Test
    public void statusIsStoppedAfterClosingAWorkerThatWasNeverRun() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = store.subscriptions().create(Company.class);

            SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, name);

            assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.NOT_STARTED);

            worker.close();

            assertThat(worker.getStatus().getState()).isEqualTo(SubscriptionWorkerState.STOPPED);
        }
    }

    @Test
    public void failingSinceUtcSurvivesTheRetryCycleAndIsClearedOnRecovery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String name = store.subscriptions().create(Company.class);

            SubscriptionWorkerOptions options = new SubscriptionWorkerOptions(name);
            // short enough to get through several attempts quickly, long enough that the clock tells
            // the timestamps of two consecutive attempts apart
            options.setTimeToWaitBeforeConnectionRetry(Duration.ofMillis(100));

            try (SubscriptionWorker<Company> worker = store.subscriptions().getSubscriptionWorker(Company.class, options)) {
                BlockingQueue<SubscriptionWorkerStatus> changes = new LinkedBlockingQueue<>();
                worker.addOnStateChanged((sender, status) -> changes.add(status));

                // no documents are ever stored here, so the state is the only indication of what the worker is doing
                worker.run(x -> { });

                // a healthy worker is not failing
                assertThat(nextStatus(changes, SubscriptionWorkerState.WAITING_FOR_DOCUMENTS).getFailingSinceUtc()).isNull();

                // a disabled database keeps every reconnect attempt failing, instead of recovering on its own
                // the way a dropped connection would
                store.maintenance().server().send(new ToggleDatabasesStateOperation(store.getDatabase(), true));
                try {
                    SubscriptionWorkerStatus first = nextStatus(changes, SubscriptionWorkerState.RETRYING);
                    assertThat(first.getFailingSinceUtc()).isNotNull();

                    // every further attempt is a state change of its own, so sinceUtc moves - but they are all the
                    // same stretch of being broken, which is what failingSinceUtc has to keep pointing at
                    SubscriptionWorkerStatus later = first;
                    while (later.getSinceUtc().equals(first.getSinceUtc())) {
                        later = nextStatus(changes, SubscriptionWorkerState.RETRYING);
                    }

                    assertThat(later.getSinceUtc()).isAfter(first.getSinceUtc());
                    assertThat(later.getFailingSinceUtc()).isEqualTo(first.getFailingSinceUtc());

                    // let it through
                    changes.clear();
                    store.maintenance().server().send(new ToggleDatabasesStateOperation(store.getDatabase(), false));

                    // CONNECTING is a step of every retry as well as the first thing a healthy worker does, so it
                    // neither starts nor ends the stretch - it carries it over
                    assertThat(nextStatus(changes, SubscriptionWorkerState.CONNECTING).getFailingSinceUtc()).isEqualTo(first.getFailingSinceUtc());

                    // and reaching the server clears it
                    assertThat(nextStatus(changes, SubscriptionWorkerState.WAITING_FOR_DOCUMENTS).getFailingSinceUtc()).isNull();
                    assertThat(worker.getStatus().getFailingSinceUtc()).isNull();
                } finally {
                    store.maintenance().server().send(new ToggleDatabasesStateOperation(store.getDatabase(), false));
                }
            }
        }
    }
}
