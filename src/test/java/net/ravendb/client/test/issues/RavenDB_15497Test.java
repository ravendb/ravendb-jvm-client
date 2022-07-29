package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.indexes.StopIndexOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenTimeoutException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_15497Test extends RemoteTestBase {

    @Test
    public void waitForIndexesAfterSaveChangesCanExitWhenThrowOnTimeoutIsFalse() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Index index = new Index();
            index.execute(store);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");
                user.setCount(3);

                session.store(user);

                session.advanced().waitForIndexesAfterSaveChanges(x -> {
                    x.withTimeout(Duration.ofSeconds(3));
                    x.throwOnTimeout(false);
                });
                session.saveChanges();
            }

            indexes.waitForIndexing(store);

            store.maintenance().send(new StopIndexOperation(index.getIndexName()));


            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");

                session.store(user);

                session.advanced().waitForIndexesAfterSaveChanges(x -> {
                    x.withTimeout(Duration.ofSeconds(3));
                    x.throwOnTimeout(true);
                });

                assertThatThrownBy(session::saveChanges)
                        .isExactlyInstanceOf(RavenTimeoutException.class)
                        .hasMessageContaining("RavenTimeoutException")
                        .hasMessageContaining("could not verify that");
            }
        }
    }

    public static class Index extends AbstractIndexCreationTask {
        public Index() {
            map = "from u in docs.Users select new { u.name }";
        }
    }
}
