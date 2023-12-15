package net.ravendb.client.documents.operations.backups;

public class StartBackupOperationResult {
    private String responsibleNode;
    private long operationId;

    public String getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(String responsibleNode) {
        this.responsibleNode = responsibleNode;
    }

    public long getOperationId() {
        return operationId;
    }

    public void setOperationId(long operationId) {
        this.operationId = operationId;
    }
}
