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

    public ShardedBatchOptions() {
    }

    public ShardedBatchOptions(ShardedBatchBehavior batchBehavior) {
        this.batchBehavior = batchBehavior;
    }

    private static final ShardedBatchOptions NON_TRANSACTIONAL_MULTI_BUCKET = new ShardedBatchOptions(ShardedBatchBehavior.NON_TRANSACTIONAL_MULTI_BUCKET);
    private static final ShardedBatchOptions TRANSACTIONAL_SINGLE_BUCKET_ONLY = new ShardedBatchOptions(ShardedBatchBehavior.TRANSACTIONAL_SINGLE_BUCKET_ONLY);

    public static ShardedBatchOptions forBehavior(ShardedBatchBehavior behavior) {
        switch (behavior) {
            case DEFAULT:
                return null;
            case TRANSACTIONAL_SINGLE_BUCKET_ONLY:
                return TRANSACTIONAL_SINGLE_BUCKET_ONLY;
            case NON_TRANSACTIONAL_MULTI_BUCKET:
                return NON_TRANSACTIONAL_MULTI_BUCKET;
            default:
                throw new IllegalArgumentException("Unhandled behavior: " + behavior);
        }
    }
}
