package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.IChangesObservable;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.changes.Observers;
import net.ravendb.client.documents.changes.OperationStatusChange;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteByQueryTest extends RemoteTestBase {

    @Test
    public void canDeleteByQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setAge(5);
                session.store(user1);

                User user2 = new User();
                user2.setAge(10);
                session.store(user2);

                session.saveChanges();
            }

            IndexQuery indexQuery = new IndexQuery();
            indexQuery.setQuery("from users where age == 5");
            DeleteByQueryOperation operation = new DeleteByQueryOperation(indexQuery);
            Operation asyncOp = store.operations().sendAsync(operation);

            asyncOp.waitForCompletion();

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.query(User.class)
                        .count())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void canDeleteByQueryWaitUsingChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setAge(5);
                session.store(user1);

                User user2 = new User();
                user2.setAge(10);
                session.store(user2);

                session.saveChanges();
            }

            Semaphore semaphore = new Semaphore(0);

            try (IDatabaseChanges changes = store.changes()) {
                changes.ensureConnectedNow();

                IChangesObservable<OperationStatusChange> allOperationChanges = changes.forAllOperations();
                allOperationChanges.subscribe(Observers.create(x -> semaphore.release()));

                IndexQuery indexQuery = new IndexQuery();
                indexQuery.setQuery("from users where age == 5");
                DeleteByQueryOperation operation = new DeleteByQueryOperation(indexQuery);
                Operation asyncOp = store.operations().sendAsync(operation);

                assertThat(semaphore.tryAcquire(15, TimeUnit.SECONDS))
                        .isTrue();

            }
        }
    }
}
