package net.ravendb.raft;

import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.client.document.DocumentStore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ClusterBasicTest extends RaftTestBase {

    @Test
    public void canCreateClusterAndSendConfiguration() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(5);
        setupClusterConfiguration(clusterStores);

        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands().forSystemDatabase(), Constants.Cluster.CLUSTER_CONFIGURATION_DOCUMENT_KEY);
            JsonDocument configurationJson = store.getDatabaseCommands().forSystemDatabase().get(Constants.Cluster.CLUSTER_CONFIGURATION_DOCUMENT_KEY);
            ClusterConfiguration configuration = JsonExtensions.createDefaultJsonSerializer().readValue(configurationJson.getDataAsJson().toString(), ClusterConfiguration.class);

            assertTrue(configuration.isEnableReplication());
        }
    }
}
