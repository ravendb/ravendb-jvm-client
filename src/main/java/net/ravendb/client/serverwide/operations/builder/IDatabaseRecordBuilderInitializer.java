package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.serverwide.DatabaseRecord;

import java.util.function.Consumer;

public interface IDatabaseRecordBuilderInitializer {
    IDatabaseRecordBuilder regular(String databaseName);
    IShardedDatabaseRecordBuilder sharded(String databaseName, Consumer<IShardedTopologyConfigurationBuilder> builder);
    DatabaseRecord toDatabaseRecord();
}
