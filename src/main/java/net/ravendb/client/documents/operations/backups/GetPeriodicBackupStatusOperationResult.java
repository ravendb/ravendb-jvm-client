package net.ravendb.client.documents.operations.backups;

public class GetPeriodicBackupStatusOperationResult extends AbstractGetPeriodicBackupStatusOperationResult {
    private PeriodicBackupStatus status;

    public PeriodicBackupStatus getStatus() {
        return status;
    }

    public void setStatus(PeriodicBackupStatus status) {
        this.status = status;
    }
}
