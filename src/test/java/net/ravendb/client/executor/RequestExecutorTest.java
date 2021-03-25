package net.ravendb.client.executor;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.UpdateTopologyParameters;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.serverwide.operations.GetDatabaseNamesOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RequestExecutorTest extends RemoteTestBase {


    @Test
    public void canIssueManyRequests() throws Exception {
        DocumentConventions conventions = new DocumentConventions();

        try (DocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), store.getDatabase(), null, null, null, store.getExecutorService(), conventions)) {
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

        try (DocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), store.getDatabase(), null, null, null, store.getExecutorService(), conventions)) {
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

        try (DocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), "no_such_db", null, null,null, store.getExecutorService(), conventions)) {

                ServerNode serverNode = new ServerNode();
                serverNode.setUrl(store.getUrls()[0]);
                serverNode.setDatabase("no_such");

                UpdateTopologyParameters updateTopologyParameters = new UpdateTopologyParameters(serverNode);
                updateTopologyParameters.setTimeoutInMs(5000);

                assertThatThrownBy(() ->
                        ExceptionsUtils.accept(() ->
                                executor.updateTopologyAsync(updateTopologyParameters).get()))
                        .isExactlyInstanceOf(DatabaseDoesNotExistException.class);
            }
        }
    }



}
