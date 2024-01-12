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
        this.noTracking = noTracking;
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
