package net.ravendb.client.documents.operations.indexes;

import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.test.client.indexing.IndexesFromClientTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexOperationsTest extends RemoteTestBase {

    @Test
    public void canDeleteIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            String[] indexNames = store.maintenance().send(new GetIndexNamesOperation(0, 10));

            assertThat(indexNames)
                    .contains("UsersIndex");

            store.maintenance().send(new DeleteIndexOperation("UsersIndex"));

            indexNames = store.maintenance().send(new GetIndexNamesOperation(0, 10));

            assertThat(indexNames)
                    .isEmpty();
        }
    }

    @Test
    public void canDisableAndEnableIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            store.maintenance().send(new DisableIndexOperation("UsersIndex"));

            IndexingStatus indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            IndexingStatus.IndexStatus indexStatus = indexingStatus.getIndexes()[0];
            assertThat(indexStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.DISABLED);

            store.maintenance().send(new EnableIndexOperation("UsersIndex"));

            indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);

            indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            indexStatus = indexingStatus.getIndexes()[0];
            assertThat(indexStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
        }
    }

    @Test
    public void getCanIndexes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);


            IndexDefinition[] indexDefinitions = store.maintenance().send(new GetIndexesOperation(0, 10));
            assertThat(indexDefinitions)
                    .hasSize(1);

        }
    }

    @Test
    public void getCanIndexesStats() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);


            IndexStats[] indexStats = store.maintenance().send(new GetIndexesStatisticsOperation());

            assertThat(indexStats)
                    .hasSize(1);
        }
    }

    @Test
    public void getTerms() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new IndexesFromClientTest.UsersIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Marcin");
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            String[] terms = store.maintenance().send(new GetTermsOperation("UsersIndex", "name", null));

            assertThat(terms)
                    .hasSize(1)
                    .contains("marcin");
        }

    }

    @Test
    public void hasIndexChanged() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            assertThat(store.maintenance().send(new IndexHasChangedOperation(indexDef)))
                    .isFalse();

            indexDef.setMaps(Sets.newHashSet("from users"));

            assertThat(store.maintenance().send(new IndexHasChangedOperation(indexDef)))
                    .isTrue();
        }
    }

    @Test
    public void canStopStartIndexing() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            store.maintenance().send(new StopIndexingOperation());

            IndexingStatus indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.PAUSED);

            store.maintenance().send(new StartIndexingOperation());

            indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);

        }
    }

    @Test
    public void canStopStartIndex() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            store.maintenance().send(new StopIndexOperation(indexDef.getName()));

            IndexingStatus indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
            assertThat(indexingStatus.getIndexes()[0].getStatus())
                    .isEqualTo(IndexRunningStatus.PAUSED);

            store.maintenance().send(new StartIndexOperation(indexDef.getName()));

            indexingStatus = store.maintenance().send(new GetIndexingStatusOperation());

            assertThat(indexingStatus.getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);
            assertThat(indexingStatus.getIndexes()[0].getStatus())
                    .isEqualTo(IndexRunningStatus.RUNNING);

        }
    }

    @Test
    public void canSetIndexLockMode() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            store.maintenance().send(new SetIndexesLockOperation(indexDef.getName(), IndexLockMode.LOCKED_ERROR));
            IndexDefinition newIndexDef = store.maintenance().send(new GetIndexOperation(indexDef.getName()));

            assertThat(newIndexDef.getLockMode())
                    .isEqualTo(IndexLockMode.LOCKED_ERROR);
        }
    }

    @Test
    public void canSetIndexPriority() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IndexesFromClientTest.UsersIndex index = new IndexesFromClientTest.UsersIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            store.maintenance().send(new SetIndexesPriorityOperation(indexDef.getName(), IndexPriority.HIGH));
            IndexDefinition newIndexDef = store.maintenance().send(new GetIndexOperation(indexDef.getName()));

            assertThat(newIndexDef.getPriority())
                    .isEqualTo(IndexPriority.HIGH);
        }
    }

    @Test
    public void canListErrors() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            UsersInvalidIndex index = new UsersInvalidIndex();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName(null);
                user.setAge(0);
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            waitForValue(() -> store.maintenance().send(new GetIndexErrorsOperation())[0].getErrors().length, 1);

            IndexErrors[] indexErrors = store.maintenance().send(new GetIndexErrorsOperation());
            IndexErrors[] perIndexErrors = store.maintenance().send(new GetIndexErrorsOperation(new String[] { indexDef.getName() }));

            assertThat(indexErrors)
                    .hasSize(1);

            assertThat(perIndexErrors)
                    .hasSize(1);

            assertThat(indexErrors[0].getErrors())
                    .hasSize(1);
            assertThat(perIndexErrors[0].getErrors())
                    .hasSize(1);
        }
    }

    @Test
    public void canGetIndexStatistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Users_Index index = new Users_Index();
            IndexDefinition indexDef = index.createIndexDefinition();

            store.maintenance().send(new PutIndexesOperation(indexDef));

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName(null);
                user.setAge(0);
                session.store(user);
                session.saveChanges();
            }

            waitForIndexing(store, store.getDatabase());

            IndexStats stats = store.maintenance().send(new GetIndexStatisticsOperation(indexDef.getName()));
            assertThat(stats.getEntriesCount())
                    .isEqualTo(1);
        }
    }

    public static class Users_Index extends AbstractIndexCreationTask {
        public Users_Index() {
            map = "from u in docs.Users select new { u.name }";
        }
    }

    public static class UsersInvalidIndex extends AbstractIndexCreationTask {
        public UsersInvalidIndex() {
            map = "from u in docs.Users select new { a = 5 / u.Age }";
        }
    }
}
