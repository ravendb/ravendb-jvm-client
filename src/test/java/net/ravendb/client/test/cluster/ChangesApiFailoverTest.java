package net.ravendb.client.test.cluster;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.changes.ISingleNodeDatabaseChanges;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ChangesApiFailoverTest extends ClusterTestBase {

    @Test
    public void changesApiShouldNotIndependentlyFailOverWhenNodeTagSpecified() throws Exception {
        // reduced port of SlowTests/Client/ChangesApiFailover.cs
        // ChangesApiShouldNotIndependentlyFailOverWhenNodeTagSpecified_OnlyRequestExecutorWillFailOver:
        // a node-specific changes connection must not fail over to another node, a non-specific one does
        try (ClusterController cluster = createRaftCluster(3)) {
            String database = getDatabaseName();
            cluster.createDatabase(new DatabaseRecord(database), 3, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                String nodeTag = store.getRequestExecutor().getPreferredNode().currentNode.getClusterTag();

                ISingleNodeDatabaseChanges nodeChanges = store.changes(database, nodeTag);
                nodeChanges.ensureConnectedNow();

                IDatabaseChanges changes = store.changes(database);
                changes.ensureConnectedNow();

                assertThat(nodeChanges.isConnected()).isTrue();
                assertThat(changes.isConnected()).isTrue();

                CountDownLatch nodeChangesDisconnected = new CountDownLatch(1);
                nodeChanges.addConnectionStatusChanged((sender, args) -> nodeChangesDisconnected.countDown());

                cluster.disposeServer(nodeTag);

                assertThat(nodeChangesDisconnected.await(30, TimeUnit.SECONDS))
                        .isTrue();

                // the non node-specific changes fail over to another node
                Boolean reconnected = waitForValue(changes::isConnected, true, java.time.Duration.ofSeconds(60));
                assertThat(reconnected)
                        .isTrue();

                // while the node-specific changes stay bound to the (dead) node
                assertThat(nodeChanges.isConnected())
                        .isFalse();
            }
        }
    }
}
