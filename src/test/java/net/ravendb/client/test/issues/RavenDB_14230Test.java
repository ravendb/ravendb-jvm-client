package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14230Test extends RemoteTestBase {

    @Test
    public void canGetNotificationAboutTimeSeriesAppend() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            BlockingQueue<TimeSeriesChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();

            changes.ensureConnectedNow();

            IChangesObservable<TimeSeriesChange> observable = changes.forTimeSeriesOfDocument("users/1");

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 33);
                    session.saveChanges();
                }

                TimeSeriesChange timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();

                assertThat(timeSeriesChange.getDocumentId())
                        .isEqualTo("users/1");
                assertThat(timeSeriesChange.getType())
                        .isEqualTo(TimeSeriesChangeTypes.PUT);
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
                assertThat(timeSeriesChange.getChangeVector())
                        .isNotNull();

                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 22);
                    session.saveChanges();
                }

                timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();

                assertThat(timeSeriesChange.getDocumentId())
                        .isEqualTo("users/1");
                assertThat(timeSeriesChange.getType())
                        .isEqualTo(TimeSeriesChangeTypes.PUT);
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
                assertThat(timeSeriesChange.getChangeVector())
                        .isNotNull();
            }
        }
    }

    @Test
    public void canGetNotificationAboutTimeSeriesDelete() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            BlockingQueue<TimeSeriesChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();

            changes.ensureConnectedNow();

            IChangesObservable<TimeSeriesChange> observable = changes.forTimeSeriesOfDocument("users/1");

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 33);
                    session.saveChanges();
                }

                TimeSeriesChange timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();

                assertThat(timeSeriesChange.getDocumentId())
                        .isEqualTo("users/1");
                assertThat(timeSeriesChange.getType())
                        .isEqualTo(TimeSeriesChangeTypes.PUT);
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
                assertThat(timeSeriesChange.getChangeVector())
                        .isNotNull();

                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .remove(null, null);
                    session.saveChanges();
                }

                timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();

                assertThat(timeSeriesChange.getDocumentId())
                        .isEqualTo("users/1");
                assertThat(timeSeriesChange.getType())
                        .isEqualTo(TimeSeriesChangeTypes.DELETE);
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
                assertThat(timeSeriesChange.getChangeVector())
                        .isNotNull();
            }
        }
    }

    @Test
    public void canSubscribeToTimeSeriesChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/1");
                session.saveChanges();
            }

            BlockingQueue<TimeSeriesChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();

            changes.ensureConnectedNow();

            IChangesObservable<TimeSeriesChange> observable = changes.forAllTimeSeries();

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 33);
                    session.saveChanges();
                }

                TimeSeriesChange timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();
                assertThat(timeSeriesChange.getCollectionName())
                        .isNotNull();
            }

            observable = changes.forTimeSeries("Likes");

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 2);
                    session.timeSeriesFor("users/1", "Dislikes")
                            .append(new Date(), 3);

                    session.saveChanges();
                }

                TimeSeriesChange timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
            }

            TimeSeriesChange timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
            assertThat(timeSeriesChange)
                    .isNull();

            observable = changes.forTimeSeriesOfDocument("users/1", "Likes");

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 4);
                    session.timeSeriesFor("users/1", "Dislikes")
                            .append(new Date(), 5);

                    session.saveChanges();
                }

                timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();
                assertThat(timeSeriesChange.getName())
                        .isEqualTo("Likes");
            }

            timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
            assertThat(timeSeriesChange)
                    .isNull();

            observable = changes.forTimeSeriesOfDocument("users/1");

            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    session.timeSeriesFor("users/1", "Likes")
                            .append(new Date(), 6);
                    session.timeSeriesFor("users/1", "Dislikes")
                            .append(new Date(), 7);

                    session.saveChanges();
                }

                timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();
                assertThat(timeSeriesChange.getName())
                        .isIn("Likes", "Dislikes");

                timeSeriesChange = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(timeSeriesChange)
                        .isNotNull();
                assertThat(timeSeriesChange.getName())
                        .isIn("Likes", "Dislikes");
            }

        }
    }
}
