package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexRunningStatus;
import net.ravendb.client.documents.indexes.IndexState;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.operations.indexes.DisableIndexOperation;
import net.ravendb.client.documents.operations.indexes.GetIndexStatisticsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.TimeoutException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_15497Test extends RemoteTestBase {

    @Test
    public void waitForIndexesAfterSaveChangesCanExitWhenThrowOnTimeoutIsFalse() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Index index = new Index();
            index.execute(store);
            store.maintenance().send(new DisableIndexOperation(index.getIndexName()));

            IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

            assertThat(indexStats.getState())
                    .isEqualTo(IndexState.DISABLED);
            assertThat(indexStats.getStatus())
                    .isEqualTo(IndexRunningStatus.DISABLED);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");

                session.store(user);

                session.advanced().waitForIndexesAfterSaveChanges(x -> {
                    x.withTimeout(Duration.ofSeconds(3));
                    x.throwOnTimeout(false);
                });
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");

                session.store(user);

                session.advanced().waitForIndexesAfterSaveChanges(x -> {
                    x.withTimeout(Duration.ofSeconds(3));
                    x.throwOnTimeout(true);
                });

                assertThatThrownBy(session::saveChanges)
                        .isExactlyInstanceOf(TimeoutException.class)
                        .hasMessageContaining("System.TimeoutException")
                        .hasMessageContaining("could not verify that 1 indexes has caught up with the changes as of etag");
            }
        }
    }

    public static class Index extends AbstractIndexCreationTask {
        public Index() {
            map = "from u in docs.Users select new { u.name }";
        }
    }
}
