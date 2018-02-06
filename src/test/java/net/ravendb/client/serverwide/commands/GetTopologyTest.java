package net.ravendb.client.serverwide.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.Topology;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetTopologyTest extends RemoteTestBase {
    @Test
    public void canGetTopology() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            GetDatabaseTopologyCommand command = new GetDatabaseTopologyCommand();

            store.getRequestExecutor().execute(command);

            Topology result = command.getResult();

            assertThat(result)
                    .isNotNull();

            assertThat(result.getEtag())
                    .isNotNull();

            assertThat(result.getNodes())
                    .hasSize(1);

            ServerNode serverNode = result.getNodes().get(0);

            assertThat(serverNode.getUrl())
                    .isEqualTo(store.getUrls()[0]);

            assertThat(serverNode.getDatabase())
                    .isEqualTo(store.getDatabase());

            assertThat(serverNode.getClusterTag())
                    .isEqualTo("A");

            assertThat(serverNode.getServerRole())
                    .isEqualTo(ServerNode.Role.MEMBER);
        }
    }
}
