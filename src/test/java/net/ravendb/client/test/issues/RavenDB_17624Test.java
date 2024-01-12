package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.exceptions.documents.subscriptions.SubscriptionDoesNotExistException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_17624Test extends RemoteTestBase {

    @Test
    public void forbidOpeningMoreThenOneSessionPerSubscriptionBatch() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                Command command1 = new Command();
                command1.setValue(1);
                session.store(command1);

                Command command2 = new Command();
                command2.setValue(2);
                session.store(command2);

                session.saveChanges();
            }

            try {
                store.subscriptions().getSubscriptionState("BackgroundSubscriptionWorker");
            } catch (SubscriptionDoesNotExistException e) {
                SubscriptionCreationOptions subscriptionCreationOptions = new SubscriptionCreationOptions();
                subscriptionCreationOptions.setName("BackgroundSubscriptionWorker");

                store.subscriptions().create(Command.class, subscriptionCreationOptions);
            }

            SubscriptionWorkerOptions workerOptions = new SubscriptionWorkerOptions("BackgroundSubscriptionWorker");

            try (SubscriptionWorker<Command> worker = store.subscriptions().getSubscriptionWorker(Command.class, workerOptions)) {

                Semaphore semaphore = new Semaphore(0);

                worker.run(batch -> {
                    try (IDocumentSession session = batch.openSession()) {
                        // empty
                    }

                    Assertions.assertThatThrownBy(batch::openSession)
                            .isExactlyInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("Session can only be opened once per each Subscription batch");

                    semaphore.release();
                });

                assertThat(semaphore.tryAcquire(15, TimeUnit.SECONDS))
                        .isTrue();
            }
        }
    }

    public static class Command {
        private String id;
        private Date processedOn;
        private int value;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getProcessedOn() {
            return processedOn;
        }

        public void setProcessedOn(Date processedOn) {
            this.processedOn = processedOn;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
