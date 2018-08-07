package net.ravendb.client.documents.session;

import net.ravendb.client.http.RequestExecutor;

public class SessionOptions {
    private String database;
    private boolean noTracking;
    private boolean noCaching;
    private RequestExecutor requestExecutor;
    private TransactionMode transactionMode;

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
}
