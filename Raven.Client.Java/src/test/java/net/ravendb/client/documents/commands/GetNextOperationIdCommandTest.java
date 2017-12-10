package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.setMaxElementsForPrinting;

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
