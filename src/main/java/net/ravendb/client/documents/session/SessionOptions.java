package net.ravendb.client.documents.session;

import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.DocumentationUrls;

/**
 * Configure the session's behavior
 */
public class SessionOptions {
    /**
     * Specify session's database, default value is taken from {@link IDocumentStore#getDatabase()}
     */
    private String database;
    /**
     * Disable tracking for all entities in the session
     * <p>For more details visit: {@link DocumentationUrls.Session.Options#NoTracking}</p>
     */
    private boolean noTracking;
    /**
     * Disable caching of HTTP responses for the session
     * <p>For more details visit: {@link DocumentationUrls.Session.Options#NoCaching}</p>
     */
    private boolean noCaching;
    private RequestExecutor requestExecutor;
    /**
     * Define the transaction mode of the session. Each {@link TransactionMode} offers a different isolation and consistency guarantees.
     * <p>For more details visit: {@link DocumentationUrls.Session.Transactions#TransactionSupport}</p>
     */
    private TransactionMode transactionMode;
    private Boolean disableAtomicDocumentWritesInClusterWideTransaction;
    /**
     * Define the consistency level for persisting changes in a sharded database.
     */
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
