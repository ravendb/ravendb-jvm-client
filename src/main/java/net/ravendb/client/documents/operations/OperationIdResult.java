package net.ravendb.client.documents.operations;

/**
 * The result of a OperationIdResult operation
 */
public class OperationIdResult {
    private long operationId;
    private String operationNodeTag;

    public String getOperationNodeTag() {
        return operationNodeTag;
    }

    public void setOperationNodeTag(String operationNodeTag) {
        this.operationNodeTag = operationNodeTag;
    }

    public long getOperationId() {
        return operationId;
    }

    public void setOperationId(long operationId) {
        this.operationId = operationId;
    }
}
