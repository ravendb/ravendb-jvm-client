package net.ravendb.executor;

import net.ravendb.RemoteTestBase;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.exceptions.DatabaseDoesNotExistException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.serverwide.operations.GetDatabaseNamesOperation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RequestExecutorTest extends RemoteTestBase {

    @Test
    public void failuresDoesNotBlockConnectionPool() {
        DocumentConventions conventions = new DocumentConventions();

        withServer((String url) -> {
            withDatabase((String db) -> {
                RequestExecutor executor = RequestExecutor.create(new String[] { url }, "no_such_db", conventions);

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
            });
        });
    }


    @Test
    public void canIssueManyRequests() {
        DocumentConventions conventions = new DocumentConventions();

        withServer((String url) -> {
            withDatabase((String db) -> {
                RequestExecutor executor = RequestExecutor.create(new String[] { url }, db, conventions);
                for (int i = 0; i < 50; i++) {
                    GetDatabaseNamesOperation databaseNamesOperation = new GetDatabaseNamesOperation(0, 20);
                    RavenCommand<String[]> command = databaseNamesOperation.getCommand(conventions);
                    executor.execute(command);
                }
            });
        });
    }

    @Test
    public void canFetchDatabasesNames() {
        DocumentConventions conventions = new DocumentConventions();

        withServer((String url) -> {
            withDatabase((String db) -> {
                RequestExecutor executor = RequestExecutor.create(new String[] { url }, db, conventions);
                GetDatabaseNamesOperation databaseNamesOperation = new GetDatabaseNamesOperation(0, 20);
                RavenCommand<String[]> command = databaseNamesOperation.getCommand(conventions);
                executor.execute(command);

                String[] dbNames = command.getResult();

                assertThat(dbNames).contains(db);
            });
        });
    }

    @Test
    public void throwsWhenUpdatingTopologyOfNotExistingDb() {
        DocumentConventions conventions = new DocumentConventions();

        withServer((String url) -> {
            RequestExecutor executor = RequestExecutor.create(new String[]{url}, "no_such_db", conventions);

            ServerNode serverNode = new ServerNode();
            serverNode.setUrl(url);
            serverNode.setDatabase("no_such");

            assertThatThrownBy(() -> {
                ExceptionsUtils.accept(() -> executor.updateTopologyAsync(serverNode, 5000).get());
            }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);
        });
    }

    @Test
    public void throwsWhenDatabaseDoesntExist() {
        DocumentConventions conventions = new DocumentConventions();

        withServer((String url) -> {
            RequestExecutor executor = RequestExecutor.create(new String[] { url }, "no_such_db", conventions);

            GetNextOperationIdCommand command = new GetNextOperationIdCommand();

            assertThatThrownBy(() -> {
                executor.execute(command);
            }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);
        });
    }

    @Test
    public void canCreateSingleNodeRequestExecutor() {
        DocumentConventions documentConventions = new DocumentConventions();

        withServer((String url) -> {
            withDatabase((String dbName) -> {
                RequestExecutor executor = RequestExecutor.createForSingleNodeWithoutConfigurationUpdates(url, dbName, documentConventions);

                List<ServerNode> nodes = executor.getTopologyNodes();

                assertThat(nodes.size()).isEqualTo(1);

                ServerNode serverNode = nodes.get(0);
                assertThat(serverNode.getUrl()).isEqualTo(url);
                assertThat(serverNode.getDatabase()).isEqualTo(dbName);

                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                executor.execute(command);

                assertThat(command.getResult()).isNotNull();
            });
        });
    }

    @Test
    public void canChooseOnlineNode() {
        DocumentConventions documentConventions = new DocumentConventions();

        withServer(url -> {
            withDatabase(dbName -> {
                RequestExecutor executor = RequestExecutor.create(new String[]{"http://no_such_host:8080", "http://another_offlilne:8080", url}, dbName, documentConventions);

                GetNextOperationIdCommand command = new GetNextOperationIdCommand();
                executor.execute(command);

                assertThat(command.getResult()).isNotNull();

                List<ServerNode> topologyNodes = executor.getTopologyNodes();

                assertThat(topologyNodes)
                        .hasSize(1)
                        .hasOnlyOneElementSatisfying(x -> x.getUrl().equals(url));

                assertThat(executor.getUrl()).isEqualTo(url);
            });
        });
    }

    @Test
    public void failsWhenServerIsOffline() {
        DocumentConventions documentConventions = new DocumentConventions();

        assertThatThrownBy(() -> {
            // don't even start server
            RequestExecutor executor = RequestExecutor.create(new String[]{"http://no_such_host:8081"}, "db1", documentConventions);


            GetNextOperationIdCommand command = new GetNextOperationIdCommand();

            executor.execute(command);
        }).isExactlyInstanceOf(AllTopologyNodesDownException.class);
    }

}
