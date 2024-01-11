package net.ravendb.client.serverwide.commands;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.OrchestratorTopology;
import net.ravendb.client.serverwide.operations.DatabaseRecordBuilder;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.sharding.AddDatabaseShardOperation;
import net.ravendb.client.serverwide.sharding.AddNodeToOrchestratorTopologyOperation;
import net.ravendb.client.serverwide.sharding.RemoveNodeFromOrchestratorTopologyOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ShardedDatabasesTest extends ClusterTestBase {

    @Test
    public void canWorkWithShardedDatabase() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            String database = getDatabaseName();
            int numberOfNodes = 3;

            // create sharded db on two nodes (A, B) and single orchestrator on C

            DatabaseRecord record = DatabaseRecordBuilder.create()
                    .sharded(database,
                            b -> b.orchestrator(o -> o.addNode("C"))
                                    .addShard(1, s -> s.addNode("A"))
                                    .addShard(2, s -> s.addNode("B")))
                    .toDatabaseRecord();

            cluster.createDatabase(record, 1, cluster.getInitialLeader().getUrl());

            try (DocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), "_")) {
                store.initialize();
                // add B as orchestrator
                // so current topology: orchestrators (B, C), shard #1 (A), shard #2 (B)

                store.maintenance().server().send(new AddNodeToOrchestratorTopologyOperation(database, "B"));

                waitForValue(() -> {
                    DatabaseRecordWithEtag r = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                    return r.getTopology() != null && r.getTopology().getMembers().contains("B");
                }, true);

                record = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                OrchestratorTopology topology = record.getSharding().getOrchestrator().getTopology();

                assertThat(topology)
                        .isNotNull();
                assertThat(topology.getMembers())
                        .contains("B")
                        .contains("C");

                // now remove C from orchestrators
                // so current topology: orchestrators (B), shard #1 (A), shard #2 (B)

                store.maintenance().server().send(new RemoveNodeFromOrchestratorTopologyOperation(database, "C"));

                record = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                topology = record.getSharding().getOrchestrator().getTopology();

                assertThat(topology)
                        .isNotNull();
                assertThat(topology.getAllNodes())
                        .hasSize(1)
                        .contains("B");

                // now add new shard
                // so current topology: orchestrators (B), shard #1 (A), shard #2 (B), shard #3 (A, B)

                store.maintenance().server().send(new AddDatabaseShardOperation(database, new String[] { "A", "B" }));

                record = store.maintenance().server().send(new GetDatabaseRecordOperation(database));
                assertThat(record.getSharding().getShards())
                        .hasSize(3);
            }
        }
    }
}
