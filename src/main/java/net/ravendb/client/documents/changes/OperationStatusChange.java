package net.ravendb.client.documents.changes;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class OperationStatusChange extends DatabaseChange {
    private long operationId;

    private ObjectNode state;

    public long getOperationId() {
        return operationId;
    }

    public void setOperationId(long operationId) {
        this.operationId = operationId;
    }

    public ObjectNode getState() {
        return state;
    }

    public void setState(ObjectNode state) {
        this.state = state;
    }
}
