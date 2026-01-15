package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public final class BackupAlreadyRunningException extends RavenException {
    private Long operationId;
    private String nodeTag;

    public BackupAlreadyRunningException(String message) {
        super(message);
    }
    public BackupAlreadyRunningException(String message, Exception innerException) { super(message, innerException); }

    public Long getOperationId() {
        return operationId;
    }

    public void setOperationId(Long operationId) {
        this.operationId = operationId;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }
}
