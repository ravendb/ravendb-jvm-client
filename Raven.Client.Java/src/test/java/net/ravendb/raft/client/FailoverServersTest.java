package net.ravendb.raft.client;

import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.data.FailoverServers;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.client.document.DocumentStore;
import net.ravendb.raft.RaftTestBase;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class FailoverServersTest extends RaftTestBase {

    @Test
    public void clientShouldHandleFailoverServers() throws IOException {
        List<DocumentStore> clusterStores = createRaftCluster(1, "Replication", null, getDbName());
        setupClusterConfiguration(clusterStores);

        try (DocumentStore store = new DocumentStore("http://localhost:12345/", clusterStores.get(0).getDefaultDatabase())) {
            store.getConventions().setClusterBehavior(ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER);
            FailoverServers failoverServers = new FailoverServers();
            ReplicationDestination replicationDestination = new ReplicationDestination();
            replicationDestination.setUrl(clusterStores.get(0).getUrl());
            replicationDestination.setDatabase(clusterStores.get(0).getDefaultDatabase());
            failoverServers.addForDefaultDatabase(replicationDestination);
            store.setFailoverServers(failoverServers);

            store.initialize();
            store.getDatabaseCommands().put("keys/1", null, new RavenJObject(), new RavenJObject());
        }

        for (DocumentStore store : clusterStores) {
            waitForDocument(store.getDatabaseCommands(), "keys/1");
        }
    }
}
