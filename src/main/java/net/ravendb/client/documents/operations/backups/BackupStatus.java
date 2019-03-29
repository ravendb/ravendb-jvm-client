package net.ravendb.client.documents.operations.backups;

import java.util.Date;

public abstract class BackupStatus {

    private Date lastFullBackup;
    private Date lastIncrementalBackup;
    private Long fullBackupDurationInMs;
    private Long incrementalBackupDurationIsMs;
    private String exception;

    public Date getLastFullBackup() {
        return lastFullBackup;
    }

    public void setLastFullBackup(Date lastFullBackup) {
        this.lastFullBackup = lastFullBackup;
    }

    public Date getLastIncrementalBackup() {
        return lastIncrementalBackup;
    }

    public void setLastIncrementalBackup(Date lastIncrementalBackup) {
        this.lastIncrementalBackup = lastIncrementalBackup;
    }

    public Long getFullBackupDurationInMs() {
        return fullBackupDurationInMs;
    }

    public void setFullBackupDurationInMs(Long fullBackupDurationInMs) {
        this.fullBackupDurationInMs = fullBackupDurationInMs;
    }

    public Long getIncrementalBackupDurationIsMs() {
        return incrementalBackupDurationIsMs;
    }

    public void setIncrementalBackupDurationIsMs(Long incrementalBackupDurationIsMs) {
        this.incrementalBackupDurationIsMs = incrementalBackupDurationIsMs;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
