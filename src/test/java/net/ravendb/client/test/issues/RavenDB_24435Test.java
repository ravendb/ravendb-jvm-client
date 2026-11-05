package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_24435Test extends RemoteTestBase {

    @Test
    public void shouldUnescapeDatabaseNameFromDatabaseMissingHeader() throws Exception {
        String databaseName = "database-møøse-" + UUID.randomUUID();

        DocumentConventions conventions = new DocumentConventions();

        try (DocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), databaseName, null, null, null, store.getExecutorService(), conventions)) {
                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                assertThatThrownBy(() -> executor.execute(command))
                        .isExactlyInstanceOf(DatabaseDoesNotExistException.class)
                        .hasMessage("Database '" + databaseName + "' does not exist.");
            }
        }
    }
}
