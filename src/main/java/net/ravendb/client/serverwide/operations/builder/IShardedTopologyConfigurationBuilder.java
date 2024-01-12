package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.OrchestratorTopology;

import java.util.function.Consumer;

public interface IShardedTopologyConfigurationBuilder {
    IShardedTopologyConfigurationBuilder orchestrator(OrchestratorTopology topology);

    IShardedTopologyConfigurationBuilder orchestrator(Consumer<IOrchestratorTopologyConfigurationBuilder> builder);

    IShardedTopologyConfigurationBuilder addShard(int shardNumber, DatabaseTopology topology);

    IShardedTopologyConfigurationBuilder addShard(int shardNumber, Consumer<IShardTopologyConfigurationBuilder> builder);
}
