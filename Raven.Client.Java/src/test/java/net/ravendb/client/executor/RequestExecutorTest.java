package net.ravendb.client.executor;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.serverwide.operations.GetDatabaseNamesOperation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RequestExecutorTest extends RemoteTestBase {

    @Test
    public void failuresDoesNotBlockConnectionPool() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), "no_such_db", null, conventions)) {
                int errorsCount = 0;

                for (int i = 0; i < 40; i++) {
                    try {
                        GetNextOperationIdCommand command = new GetNextOperationIdCommand();
                        executor.execute(command);
                    } catch (Exception e) {
                        errorsCount++;
                    }
                }

                assertThat(errorsCount).isEqualTo(40);

                GetDatabaseNamesOperation databaseNamesOperation = new GetDatabaseNamesOperation(0, 20);
                RavenCommand<String[]> command = databaseNamesOperation.getCommand(conventions);
                executor.execute(command);

                assertThat(command.getResult()).doesNotContainNull();
            }
        }
    }


    @Test
    public void canIssueManyRequests() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), store.getDatabase(), null, conventions)) {
                for (int i = 0; i < 50; i++) {
                    GetDatabaseNamesOperation databaseNamesOperation = new GetDatabaseNamesOperation(0, 20);
                    RavenCommand<String[]> command = databaseNamesOperation.getCommand(conventions);
                    executor.execute(command);
                }
            }
        }
    }

    @Test
    public void canFetchDatabasesNames() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), store.getDatabase(), null, conventions)) {
                GetDatabaseNamesOperation databaseNamesOperation = new GetDatabaseNamesOperation(0, 20);
                RavenCommand<String[]> command = databaseNamesOperation.getCommand(conventions);
                executor.execute(command);

                String[] dbNames = command.getResult();

                assertThat(dbNames).contains(store.getDatabase());
            }
        }
    }

    @Test
    public void throwsWhenUpdatingTopologyOfNotExistingDb() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), "no_such_db", null, conventions)) {

                ServerNode serverNode = new ServerNode();
                serverNode.setUrl(store.getUrls()[0]);
                serverNode.setDatabase("no_such");

                assertThatThrownBy(() -> {
                    ExceptionsUtils.accept(() -> executor.updateTopologyAsync(serverNode, 5000).get());
                }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);
            }
        }
    }

    @Test
    public void throwsWhenDatabaseDoesntExist() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), "no_such_db", null, conventions)) {

                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                assertThatThrownBy(() -> {
                    executor.execute(command);
                }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);
            }
        }
    }

    @Test
    public void canCreateSingleNodeRequestExecutor() throws Exception {
        DocumentConventions documentConventions = new DocumentConventions();
        try (IDocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.createForSingleNodeWithoutConfigurationUpdates(store.getUrls()[0], store.getDatabase(), null, documentConventions)) {

                List<ServerNode> nodes = executor.getTopologyNodes();

                assertThat(nodes.size()).isEqualTo(1);

                ServerNode serverNode = nodes.get(0);
                assertThat(serverNode.getUrl()).isEqualTo(store.getUrls()[0]);
                assertThat(serverNode.getDatabase()).isEqualTo(store.getDatabase());

                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                executor.execute(command);

                assertThat(command.getResult()).isNotNull();
            }
        }
    }

    @Test
    public void canChooseOnlineNode() throws Exception {
        DocumentConventions documentConventions = new DocumentConventions();

        try (IDocumentStore store = getDocumentStore()) {
            String url = store.getUrls()[0];
            String dbName = store.getDatabase();

            try (RequestExecutor executor = RequestExecutor.create(new String[]{"http://no_such_host:8080", "http://another_offlilne:8080", url}, dbName, null, documentConventions)) {
                GetNextOperationIdCommand command = new GetNextOperationIdCommand();
                executor.execute(command);

                assertThat(command.getResult()).isNotNull();

                List<ServerNode> topologyNodes = executor.getTopologyNodes();

                assertThat(topologyNodes)
                        .hasSize(1)
                        .hasOnlyOneElementSatisfying(x -> x.getUrl().equals(url));

                assertThat(executor.getUrl()).isEqualTo(url);
            }
        }
    }

    @Test
    public void failsWhenServerIsOffline() {
        DocumentConventions documentConventions = new DocumentConventions();

        assertThatThrownBy(() -> {
            // don't even start server
            try (RequestExecutor executor = RequestExecutor.create(new String[]{"http://no_such_host:8081"}, "db1", null, documentConventions)) {
                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                executor.execute(command);
            }
        }).isExactlyInstanceOf(AllTopologyNodesDownException.class);
    }

}
