package net.ravendb.client.documents.commands.batches;

public class BatchOptions {

    private ReplicationBatchOptions replicationOptions;
    private IndexBatchOptions indexOptions;

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
}
