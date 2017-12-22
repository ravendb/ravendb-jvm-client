package net.ravendb.client.documents.operations;

/**
 * The result of a OperationIdResult operation
 */
public class OperationIdResult {
    private long operationId;

    public long getOperationId() {
        return operationId;
    }

    public void setOperationId(long operationId) {
        this.operationId = operationId;
    }
}
