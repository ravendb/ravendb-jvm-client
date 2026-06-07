package net.ravendb.client.documents.session;

import net.ravendb.client.http.RequestExecutor;

public class SessionOptions {
    private String database;
    private boolean noTracking;
    private boolean noCaching;
    private RequestExecutor requestExecutor;
    private TransactionMode transactionMode;
    private Boolean disableAtomicDocumentWritesInClusterWideTransaction;
    private ShardedBatchBehavior shardedBatchBehavior;
    private OptimisticConcurrencyMode optimisticConcurrencyMode;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public RequestExecutor getRequestExecutor() {
        return requestExecutor;
    }

    public void setRequestExecutor(RequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    public boolean isNoTracking() {
        return noTracking;
    }

    public void setNoTracking(boolean noTracking) {
        if (noTracking && optimisticConcurrencyMode != null && optimisticConcurrencyMode != OptimisticConcurrencyMode.NONE) {
            throw new IllegalStateException("noTracking cannot be set to true when optimisticConcurrencyMode is " + optimisticConcurrencyMode + ".");
        }

        this.noTracking = noTracking;
    }

    /**
     * Configure optimistic concurrency mode for the session.
     * When set, overrides the default from {@code DocumentConventions.optimisticConcurrencyMode}.
     * When {@code null} (default), the session inherits the value from conventions.
     * @return optimistic concurrency mode
     */
    public OptimisticConcurrencyMode getOptimisticConcurrencyMode() {
        return optimisticConcurrencyMode;
    }

    /**
     * Configure optimistic concurrency mode for the session.
     * When set, overrides the default from {@code DocumentConventions.optimisticConcurrencyMode}.
     * When {@code null} (default), the session inherits the value from conventions.
     * @param optimisticConcurrencyMode value to set
     */
    public void setOptimisticConcurrencyMode(OptimisticConcurrencyMode optimisticConcurrencyMode) {
        if (optimisticConcurrencyMode != null && optimisticConcurrencyMode != OptimisticConcurrencyMode.NONE
                && transactionMode == TransactionMode.CLUSTER_WIDE) {
            throw new IllegalStateException("optimisticConcurrencyMode cannot be set to " + optimisticConcurrencyMode
                    + " when transactionMode is " + TransactionMode.CLUSTER_WIDE + ".");
        }

        if (optimisticConcurrencyMode != null && optimisticConcurrencyMode != OptimisticConcurrencyMode.NONE && noTracking) {
            throw new IllegalStateException("optimisticConcurrencyMode cannot be set to " + optimisticConcurrencyMode
                    + " when noTracking is true.");
        }

        this.optimisticConcurrencyMode = optimisticConcurrencyMode;
    }

    public boolean isNoCaching() {
        return noCaching;
    }

    public void setNoCaching(boolean noCaching) {
        this.noCaching = noCaching;
    }

    public TransactionMode getTransactionMode() {
        return transactionMode;
    }

    public void setTransactionMode(TransactionMode transactionMode) {
        if (transactionMode == TransactionMode.CLUSTER_WIDE
                && optimisticConcurrencyMode != null && optimisticConcurrencyMode != OptimisticConcurrencyMode.NONE) {
            throw new IllegalStateException("optimisticConcurrencyMode cannot be set to " + optimisticConcurrencyMode
                    + " when transactionMode is " + TransactionMode.CLUSTER_WIDE + ".");
        }

        this.transactionMode = transactionMode;
    }

    /**
     * EXPERT: Disable automatic atomic writes with cluster write transactions. If set to 'true',
     * will only consider explicitly added compare exchange values to validate cluster wide transactions.
     * @return disable atomic writes
     */
    public Boolean getDisableAtomicDocumentWritesInClusterWideTransaction() {
        return disableAtomicDocumentWritesInClusterWideTransaction;
    }

    /**
     * EXPERT: Disable automatic atomic writes with cluster write transactions. If set to 'true',
     * will only consider explicitly added compare exchange values to validate cluster wide transactions.
     * @param disableAtomicDocumentWritesInClusterWideTransaction disable atomic writes
     */
    public void setDisableAtomicDocumentWritesInClusterWideTransaction(Boolean disableAtomicDocumentWritesInClusterWideTransaction) {
        this.disableAtomicDocumentWritesInClusterWideTransaction = disableAtomicDocumentWritesInClusterWideTransaction;
    }

    public ShardedBatchBehavior getShardedBatchBehavior() {
        return shardedBatchBehavior;
    }

    public void setShardedBatchBehavior(ShardedBatchBehavior shardedBatchBehavior) {
        this.shardedBatchBehavior = shardedBatchBehavior;
    }
}
