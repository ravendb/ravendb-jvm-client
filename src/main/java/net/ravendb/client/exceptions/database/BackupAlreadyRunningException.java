package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public final class BackupAlreadyRunningException extends RavenException {

    private long operationId;
    private String nodeTag;

    public BackupAlreadyRunningException(String message) {
        super(message);
    }

    public BackupAlreadyRunningException(String message, Throwable cause) {
        super(message, cause);
    }

    public long getOperationId() {
        return operationId;
    }

    public void setOperationId(long operationId) {
        this.operationId = operationId;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }
}
