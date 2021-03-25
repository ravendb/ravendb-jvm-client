package net.ravendb.client.test.server.documents.notifications;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.*;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexPriority;
import net.ravendb.client.documents.operations.indexes.SetIndexesPriorityOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.infrastructure.entities.Order;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ChangesTest extends RemoteTestBase {

    @Test
    public void singleDocumentChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();

            changes.ensureConnectedNow();

            IChangesObservable<DocumentChange> observable = changes.forDocument("users/1");
            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                DocumentChange documentChange = changesList.poll(2, TimeUnit.SECONDS);
                assertThat(documentChange)
                        .isNotNull();

                assertThat(documentChange.getId())
                        .isEqualTo("users/1");

                assertThat(documentChange.getCollectionName())
                        .isEqualTo("Users");

                assertThat(documentChange.getType())
                        .isEqualTo(DocumentChangeTypes.PUT);

                DocumentChange secondPoll = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(secondPoll)
                        .isNull();
            }


            // at this point we should be unsubscribed from changes on 'users/1'

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("another name");
                session.store(user, "users/1");
                session.saveChanges();
            }

            // it should be empty as we destroyed subscription
            DocumentChange thirdPoll = changesList.poll(1, TimeUnit.SECONDS);
            assertThat(thirdPoll)
                    .isNull();
        }
    }

    @Test
    public void changesWithHttps() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {

            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();
            changes.ensureConnectedNow();

            IChangesObservable<DocumentChange> observable = changes.forDocument("users/1");
            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                DocumentChange documentChange = changesList.poll(2, TimeUnit.SECONDS);
                assertThat(documentChange)
                        .isNotNull();

                assertThat(documentChange.getId())
                        .isEqualTo("users/1");

                assertThat(documentChange.getType())
                        .isEqualTo(DocumentChangeTypes.PUT);

                DocumentChange secondPoll = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(secondPoll)
                        .isNull();
            }

            // at this point we should be unsubscribed from changes on 'users/1'

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("another name");
                session.store(user, "users/1");
                session.saveChanges();
            }

            // it should be empty as we destroyed subscription
            DocumentChange thirdPoll = changesList.poll(1, TimeUnit.SECONDS);
            assertThat(thirdPoll)
                    .isNull();

        }
    }

    @Test
    public void allDocumentsChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();
            changes.ensureConnectedNow();

            IChangesObservable<DocumentChange> observable = changes.forAllDocuments();
            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {

                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    session.store(user, "users/1");
                    session.saveChanges();
                }

                DocumentChange documentChange = changesList.poll(2, TimeUnit.SECONDS);
                assertThat(documentChange)
                        .isNotNull();

                assertThat(documentChange.getId())
                        .isEqualTo("users/1");

                assertThat(documentChange.getType())
                        .isEqualTo(DocumentChangeTypes.PUT);

                DocumentChange secondPoll = changesList.poll(1, TimeUnit.SECONDS);
                assertThat(secondPoll)
                        .isNull();
            }


            // at this point we should be unsubscribed from changes on 'users/1'

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("another name");
                session.store(user, "users/1");
                session.saveChanges();
            }

            // it should be empty as we destroyed subscription
            DocumentChange thirdPoll = changesList.poll(1, TimeUnit.SECONDS);
            assertThat(thirdPoll)
                    .isNull();

        }
    }

    public static class UsersByName extends AbstractIndexCreationTask {
        public UsersByName() {

            map = "from c in docs.Users select new " +
                    " {" +
                    "    c.name, " +
                    "    count = 1" +
                    "}";

            reduce = "from result in results " +
                    "group result by result.name " +
                    "into g " +
                    "select new " +
                    "{ " +
                    "  name = g.Key, " +
                    "  count = g.Sum(x => x.count) " +
                    "}";
        }
    }

    @Test
    public void singleIndexChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            store.executeIndex(new UsersByName());

            BlockingQueue<IndexChange> changesList = new BlockingArrayQueue<>();

            try (IDatabaseChanges changes = store.changes()) {

                changes.ensureConnectedNow();

                IChangesObservable<IndexChange> observable = changes.forIndex(new UsersByName().getIndexName());

                try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {

                    Thread.sleep(500);
                    SetIndexesPriorityOperation operation = new SetIndexesPriorityOperation(new UsersByName().getIndexName(), IndexPriority.LOW);
                    store.maintenance().send(operation);

                    IndexChange indexChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(indexChange)
                            .isNotNull();

                    assertThat(indexChange.getName())
                            .isEqualTo(new UsersByName().getIndexName());
                }
            }
        }
    }

    @Test
    public void allIndexChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            store.executeIndex(new UsersByName());

            BlockingQueue<IndexChange> changesList = new BlockingArrayQueue<>();

            try (IDatabaseChanges changes = store.changes()) {
                changes.ensureConnectedNow();

                IChangesObservable<IndexChange> observable = changes.forAllIndexes();

                try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {

                    Thread.sleep(500);
                    SetIndexesPriorityOperation operation = new SetIndexesPriorityOperation(new UsersByName().getIndexName(), IndexPriority.LOW);
                    store.maintenance().send(operation);

                    IndexChange indexChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(indexChange)
                            .isNotNull();

                    assertThat(indexChange.getName())
                            .isEqualTo(new UsersByName().getIndexName());
                }
            }
        }
    }

    @Test
    public void canCanNotificationAboutDocumentsStartingWith() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            try (IDatabaseChanges changes = store.changes()) {
                changes.ensureConnectedNow();

                IChangesObservable<DocumentChange> observable = changes.forDocumentsStartingWith("users/");

                try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                    try (IDocumentSession session = store.openSession()) {
                        session.store(new User(), "users/1");
                        session.saveChanges();
                    }

                    try (IDocumentSession session = store.openSession()) {
                        session.store(new User(), "differentDocumentPrefix/1");
                        session.saveChanges();
                    }

                    try (IDocumentSession session = store.openSession()) {
                        session.store(new User(), "users/2");
                        session.saveChanges();
                    }

                    DocumentChange documentChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(documentChange)
                            .isNotNull();

                    assertThat(documentChange.getId())
                            .isEqualTo("users/1");

                    documentChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(documentChange)
                            .isNotNull();

                    assertThat(documentChange.getId())
                            .isEqualTo("users/2");
                }
            }
        }
    }

    @Test
    public void canCanNotificationAboutDocumentsFromCollection() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            try (IDatabaseChanges changes = store.changes()) {
                changes.ensureConnectedNow();

                IChangesObservable<DocumentChange> observable = changes.forDocumentsInCollection("users");

                try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                    try (IDocumentSession session = store.openSession()) {
                        session.store(new User(), "users/1");
                        session.saveChanges();
                    }

                    try (IDocumentSession session = store.openSession()) {
                        session.store(new Order(), "orders/1");
                        session.saveChanges();
                    }

                    try (IDocumentSession session = store.openSession()) {
                        session.store(new User(), "users/2");
                        session.saveChanges();
                    }

                    DocumentChange documentChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(documentChange)
                            .isNotNull();

                    assertThat(documentChange.getId())
                            .isEqualTo("users/1");

                    documentChange = changesList.poll(2, TimeUnit.SECONDS);

                    assertThat(documentChange)
                            .isNotNull();

                    assertThat(documentChange.getId())
                            .isEqualTo("users/2");
                }
            }
        }
    }

    @Test
    public void notificationOnWrongDatabase_ShouldNotCrashServer() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IDatabaseChanges changes = store.changes("no_such_db");

            assertThatThrownBy(() -> changes.ensureConnectedNow())
                    .isExactlyInstanceOf(DatabaseDoesNotExistException.class);

            store.maintenance().send(new GetStatisticsOperation());
        }
    }
}
