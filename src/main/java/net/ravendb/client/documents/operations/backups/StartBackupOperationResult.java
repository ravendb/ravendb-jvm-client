package net.ravendb.client.documents.operations.backups;

public class StartBackupOperationResult {
    private String responsibleNode;
    private int operationId;

    public String getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(String responsibleNode) {
        this.responsibleNode = responsibleNode;
    }

    public int getOperationId() {
        return operationId;
    }

    public void setOperationId(int operationId) {
        this.operationId = operationId;
    }
}
