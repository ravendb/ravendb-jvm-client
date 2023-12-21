package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.documents.session.ShardedBatchBehavior;

public class ShardedBatchOptions {

    private ShardedBatchBehavior batchBehavior;

    public ShardedBatchBehavior getBatchBehavior() {
        return batchBehavior;
    }

    public void setBatchBehavior(ShardedBatchBehavior batchBehavior) {
        this.batchBehavior = batchBehavior;
    }
}
