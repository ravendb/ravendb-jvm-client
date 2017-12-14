package net.ravendb.client.documents.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetNextOperationIdCommandTest extends RemoteTestBase {

    @Test
    public void canGetNextOperationId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            GetNextOperationIdCommand command = new GetNextOperationIdCommand();

            store.getRequestExecutor().execute(command);

            assertThat(command.getResult()).isNotNull();
        }
    }
}
