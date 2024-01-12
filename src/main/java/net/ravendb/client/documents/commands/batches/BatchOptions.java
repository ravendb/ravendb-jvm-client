package net.ravendb.client.documents.commands.batches;

public class BatchOptions {

    private ReplicationBatchOptions replicationOptions;
    private IndexBatchOptions indexOptions;

    private ShardedBatchOptions shardedOptions;

    public ReplicationBatchOptions getReplicationOptions() {
        return replicationOptions;
    }

    public void setReplicationOptions(ReplicationBatchOptions replicationOptions) {
        this.replicationOptions = replicationOptions;
    }

    public IndexBatchOptions getIndexOptions() {
        return indexOptions;
    }

    public void setIndexOptions(IndexBatchOptions indexOptions) {
        this.indexOptions = indexOptions;
    }

    public ShardedBatchOptions getShardedOptions() {
        return shardedOptions;
    }

    public void setShardedOptions(ShardedBatchOptions shardedOptions) {
        this.shardedOptions = shardedOptions;
    }
}
