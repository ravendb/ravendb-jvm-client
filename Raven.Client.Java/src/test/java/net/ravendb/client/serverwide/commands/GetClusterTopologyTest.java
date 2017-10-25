package net.ravendb.client.serverwide.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.http.ClusterTopology;
import net.ravendb.client.http.ClusterTopologyResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GetClusterTopologyTest extends RemoteTestBase {
    @Test
    public void canGetTopology() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            GetClusterTopologyCommand command = new GetClusterTopologyCommand();

            store.getRequestExecutor().execute(command);

            ClusterTopologyResponse result = command.getResult();

            assertThat(result)
                    .isNotNull();

            assertThat(result.getLeader())
                    .isNotEmpty();

            assertThat(result.getNodeTag())
                    .isNotEmpty();

            ClusterTopology topology = result.getTopology();

            assertThat(topology)
                    .isNotNull();

            assertThat(topology.getTopologyId())
                    .isNotNull();

            assertThat(topology.getMembers())
                    .hasSize(1);

            assertThat(topology.getWatchers())
                    .hasSize(0);

            assertThat(topology.getPromotables())
                    .hasSize(0);
        }
    }
}
