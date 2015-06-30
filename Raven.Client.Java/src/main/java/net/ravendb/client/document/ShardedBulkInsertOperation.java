package net.ravendb.client.document;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.shard.IShardResolutionStrategy;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.shard.ShardedDocumentStore;

import java.util.HashMap;
import java.util.Map;

public class ShardedBulkInsertOperation implements CleanCloseable {

    private final GenerateEntityIdOnTheClient generateEntityIdOnTheClient;
    private final ShardedDocumentStore shardedDocumentStore;
    private IDatabaseCommands databaseCommands;
    public Map<String, IDocumentStore> shards;
    private String database;
    private final BulkInsertOptions options;
    private final IShardResolutionStrategy shardResolutionStrategy;
    private final ShardStrategy shardStrategy;
    //key - shardId - value - bulk
    private Map<String, BulkInsertOperation> bulks;

    public ShardedBulkInsertOperation(final String database, final ShardedDocumentStore shardedDocumentStore, BulkInsertOptions options) {
        this.database = database;
        this.shardedDocumentStore = shardedDocumentStore;
        this.options = options;
        shards = shardedDocumentStore.getShardStrategy().getShards();
        bulks = new HashMap<>();
        generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(shardedDocumentStore.getConventions(), new Function1<Object, String>() {
            @Override
            public String apply(Object entity) {
                return shardedDocumentStore.getConventions().generateDocumentKey(database, databaseCommands, entity);
            }
        });
        shardResolutionStrategy = shardedDocumentStore.getShardStrategy().getShardResolutionStrategy();
        shardStrategy = shardedDocumentStore.getShardStrategy();
    }

    public boolean isAborted() {
        for (BulkInsertOperation op: bulks.values()) {
            if (op.isAborted()) {
                return true;
            }
        }
        return false;
    }

    public void abort() {
        for (BulkInsertOperation op: bulks.values()) {
            op.abort();
        }
    }

    public IDatabaseCommands getDatabaseCommands() {
        return databaseCommands;
    }

    public void store(Object entity) throws InterruptedException {
        String shardId = shardResolutionStrategy.generateShardIdFor(entity, this);
        databaseCommands = shards.get(shardId).getDatabaseCommands();

        database = MultiDatabase.getDatabaseName(shards.get(shardId).getUrl());

        Reference<String> idRef = new Reference<>();
        if (!generateEntityIdOnTheClient.tryGetIdFromInstance(entity, idRef)) {
            idRef.value = generateEntityIdOnTheClient.getOrGenerateDocumentKey(entity);
        }

        String modifyDocumentId = shardStrategy.getModifyDocumentId().apply(shardedDocumentStore.getConventions(), shardId, idRef.value);
        BulkInsertOperation bulkInsertOperation = bulks.get(shardId);
        if (bulkInsertOperation == null) {
            IDocumentStore shard = shards.get(shardId);
            bulkInsertOperation = new BulkInsertOperation(database, shard, shard.getListeners(), options, shard.changes());
            bulks.put(shardId, bulkInsertOperation);
        }

        bulkInsertOperation.store(entity, modifyDocumentId);
    }

    @Override
    public void close() {
        for (BulkInsertOperation op: bulks.values()) {
            op.close();
        }

    }
}
