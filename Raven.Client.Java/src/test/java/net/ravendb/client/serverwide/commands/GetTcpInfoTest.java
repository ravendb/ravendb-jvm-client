package net.ravendb.client.serverwide.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GetTcpInfoTest extends RemoteTestBase {
    @Test
    public void canGetTcpInfo() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            GetTcpInfoCommand command = new GetTcpInfoCommand("test");

            store.getRequestExecutor().execute(command);

            TcpConnectionInfo result = command.getResult();

            assertThat(result)
                    .isNotNull();

            assertThat(result.getCertificate())
                    .isNull();

            assertThat(result.getPort())
                    .isNotNull();

            assertThat(result.getUrl())
                    .isNotNull();
        }
    }
}
