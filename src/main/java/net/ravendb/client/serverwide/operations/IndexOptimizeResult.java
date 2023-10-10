package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.operations.IOperationResult;

public class IndexOptimizeResult implements IOperationResult {
    private String indexName;
    private String message;
    private boolean shouldPersist;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isShouldPersist() {
        return shouldPersist;
    }

    public void setShouldPersist(boolean shouldPersist) {
        this.shouldPersist = shouldPersist;
    }

}
