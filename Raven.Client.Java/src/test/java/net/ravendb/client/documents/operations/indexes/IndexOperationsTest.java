package net.ravendb.client.documents.operations.indexes;

import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.test.client.indexing.IndexesFromClientTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexOperationsTest extends RemoteTestBase {

    @Test
    public void canDeleteIndex() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            String[] indexNames = store.admin().send(new GetIndexNamesOperation(0, 10));

            assertThat(indexNames)
                    .contains("UsersIndex");

            store.admin().send(new DeleteIndexOperation("UsersIndex"));

            indexNames = store.admin().send(new GetIndexNamesOperation(0, 10));

            assertThat(indexNames)
                    .isEmpty();
        }
    }

    @Test
    public void canDisableAndEnableIndex() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            store.admin().send(new DisableIndexOperation("UsersIndex"));

            IndexingStatus indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            IndexingStatus.IndexStatus indexStatus = indexingStatus.getIndexes()[0];
            assertThat(indexStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.DISABLED);

            store.admin().send(new EnableIndexOperation("UsersIndex"));

            indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            indexStatus = indexingStatus.getIndexes()[0];
            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
        }
    }

    @Test
    public void getCanIndexes() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);


            IndexDefinition[] indexDefinitions = store.admin().send(new GetIndexesOperation(0, 10));
            assertThat(indexDefinitions)
                    .hasSize(1);

        }
    }

    @Test
    public void getCanIndexesStats() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);


            IndexStats[] indexStats = store.admin().send(new GetIndexesStatisticsOperation());

            assertThat(indexStats)
                    .hasSize(1);
        }
    }

    @Test
    public void getTerms() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Marcin");
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            String[] terms = store.admin().send(new GetTermsOperation("UsersIndex", "Name", null));

            assertThat(terms)
                    .hasSize(1)
                    .contains("marcin");
        }

    }

    @Test
    public void hasIndexChanged() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            assertThat(store.admin().send(new IndexHasChangedOperation(indexDef)))
                    .isFalse();

            indexDef.setMaps(Sets.newHashSet("from users"));

            assertThat(store.admin().send(new IndexHasChangedOperation(indexDef)))
                    .isTrue();
        }
    }

    @Test
    public void canStopStartIndexing() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            store.admin().send(new StopIndexingOperation());

            IndexingStatus indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.PAUSED);

            store.admin().send(new StartIndexingOperation());

            indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);

        }
    }

    @Test
    public void canStopStartIndex() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            store.admin().send(new StopIndexOperation(indexDef.getName()));

            IndexingStatus indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
            assertThat(indexingStatus.getIndexes()[0].getStatus())
                    .isEqualTo(IndexRunningStatus.PAUSED);

            store.admin().send(new StartIndexOperation(indexDef.getName()));

            indexingStatus = store.admin().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
            assertThat(indexingStatus.getIndexes()[0].getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);

        }
    }

    @Test
    public void canSetIndexLockMode() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            store.admin().send(new SetIndexesLockOperation(indexDef.getName(), IndexLockMode.LOCKED_ERROR));
            IndexDefinition newIndexDef = store.admin().send(new GetIndexOperation(indexDef.getName()));

            assertThat(newIndexDef.getLockMode())
                    .isEqualTo(IndexLockMode.LOCKED_ERROR);
        }
    }

    @Test
    public void canSetIndexPriority() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            store.admin().send(new SetIndexesPriorityOperation(indexDef.getName(), IndexPriority.HIGH));
            IndexDefinition newIndexDef = store.admin().send(new GetIndexOperation(indexDef.getName()));

            assertThat(newIndexDef.getPriority())
                    .isEqualTo(IndexPriority.HIGH);
        }
    }

    @Test
    public void canListErrors() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            UsersInvalidIndex index = new UsersInvalidIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName(null);
                user.setAge(0);
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            IndexErrors[] indexErrors = store.admin().send(new GetIndexErrorsOperation());
            IndexErrors[] perIndexErrors = store.admin().send(new GetIndexErrorsOperation(new String[] { indexDef.getName() }));

            assertThat(indexErrors)
                    .hasSize(1);

            assertThat(perIndexErrors)
                    .hasSize(1);
        }
    }

    @Test
    public void canGetIndexStatistics() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {
            Users_Index index = new Users_Index();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.admin().send(new PutIndexesOperation(indexDef));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName(null);
                user.setAge(0);
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            IndexStats stats = store.admin().send(new GetIndexStatisticsOperation(indexDef.getName()));
            assertThat(stats.getEntriesCount())
                    .isEqualTo(1);
        }
    }

    public static class Users_Index extends AbstractIndexCreationTask {
        public Users_Index() {
            map = "from u in docs.Users select new { u.Name }";
        }
    }

    public static class UsersInvalidIndex extends AbstractIndexCreationTask {
        public UsersInvalidIndex() {
            map = "from u in docs.Users select new { A = 5 / u.Age }";
        }
    }
}
