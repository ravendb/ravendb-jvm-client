package net.ravendb.operations;

import net.ravendb.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GetNextOperationIdCommandTest extends RemoteTestBase {

    @Test
    public void canGetNextOperationId() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            GetNextOperationIdCommand command = new GetNextOperationIdCommand();

            store.getRequestExecutor().execute(command);

            assertThat(command.getResult()).isNotNull();
        }
    }
}
