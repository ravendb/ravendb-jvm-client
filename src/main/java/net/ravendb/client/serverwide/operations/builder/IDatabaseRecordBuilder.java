package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.serverwide.DatabaseTopology;

import java.util.function.Consumer;

public interface IDatabaseRecordBuilder extends IDatabaseRecordBuilderBase {
    IDatabaseRecordBuilderBase withTopology(DatabaseTopology topology);
    IDatabaseRecordBuilderBase withTopology(Consumer<ITopologyConfigurationBuilder> builder);
    IDatabaseRecordBuilderBase withReplicationFactor(int replicationFactor);
}
