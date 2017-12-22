package net.ravendb.client.test.client.indexing;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.ExplainQueryCommand;
import net.ravendb.client.documents.commands.GetStatisticsCommand;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.indexes.DeleteIndexOperation;
import net.ravendb.client.documents.operations.indexes.GetIndexNamesOperation;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexesFromClientTest extends RemoteTestBase {


    @Test
    public void canReset() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user1 = new User();
                user1.setName("Marcin");
                session.store(user1, "users/1");
                session.saveChanges();
            }

            store.executeIndex(new UsersIndex());

            waitForIndexing(store);

            GetStatisticsCommand command = new GetStatisticsCommand();
            store.getRequestExecutor().execute(command);

            DatabaseStatistics statistics = command.getResult();
            Date firstIndexingTime = statistics.getIndexes()[0].getLastIndexingTime();

            String indexName = new UsersIndex().getIndexName();

            // now reset index

            Thread.sleep(2); /// avoid the same millisecond

            store.maintenance().send(new ResetIndexOperation(indexName));
            waitForIndexing(store);

            command = new GetStatisticsCommand();
            store.getRequestExecutor().execute(command);

            statistics = command.getResult();

            Date secondIndexingTime = statistics.getLastIndexingTime();
            assertThat(firstIndexingTime)
                    .isBefore(secondIndexingTime);
        }
    }

    @Test
    public void canExecuteManyIndexes() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndexes(Collections.singletonList(new UsersIndex()));

            GetIndexNamesOperation indexNamesOperation = new GetIndexNamesOperation(0, 10);
            String[] indexNames = store.maintenance().send(indexNamesOperation);

            assertThat(indexNames)
                    .hasSize(1);
        }
    }

    public static class UsersIndex extends AbstractIndexCreationTask {
        public UsersIndex() {
            map = "from user in docs.users select new { user.name }";
        }
    }

    @Test
    public void canDelete() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.executeIndex(new UsersIndex());

            store.maintenance().send(new DeleteIndexOperation(new UsersIndex().getIndexName()));

            GetStatisticsCommand command = new GetStatisticsCommand();
            store.getRequestExecutor().execute(command);

            DatabaseStatistics statistics = command.getResult();

            assertThat(statistics.getIndexes())
                    .hasSize(0);
        }
    }

    //TBD public async Task CanStopAndStart()
    //TBD public async Task SetLockModeAndSetPriority()
    //TBD public async Task GetErrors()
    //TBD public async Task GetDefinition()
    //TBD public async Task GetTerms()
    //TBD public async Task Performance()
    //TBD public async Task GetIndexNames()

    @Test
    public void canExplain() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            User user1 = new User();
            user1.setName("Fitzchak");

            User user2 = new User();
            user2.setName("Arek");

            try (IDocumentSession session = store.openSession()) {
                session.store(user1);
                session.store(user2);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Reference<QueryStatistics> statsRef = new Reference<>();
                List<User> users = session.query(User.class)
                        .statistics(statsRef)
                        .whereEquals("name", "Arek")
                        .toList();

                users = session.query(User.class)
                        .statistics(statsRef)
                        .whereGreaterThan("age", 10)
                        .toList();
            }

            IndexQuery indexQuery = new IndexQuery("from users");
            ExplainQueryCommand command = new ExplainQueryCommand(store.getConventions(), indexQuery);

            store.getRequestExecutor().execute(command);

            ExplainQueryCommand.ExplainQueryResult[] explanations = command.getResult();
            assertThat(explanations)
                    .hasSize(1);
            assertThat(explanations[0].getIndex())
                    .isNotNull();
            assertThat(explanations[0].getReason())
                    .isNotNull();
        }
    }

    //TBD public async Task MoreLikeThis()
}
