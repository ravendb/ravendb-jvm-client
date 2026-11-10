package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

public abstract class BackupStatus {

    private Date lastFullBackup;
    private Date lastIncrementalBackup;
    private Long fullBackupDurationInMs;
    private Long incrementalBackupDurationInMs;
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

    public Long getIncrementalBackupDurationInMs() {
        return incrementalBackupDurationInMs;
    }

    public void setIncrementalBackupDurationInMs(Long incrementalBackupDurationInMs) {
        this.incrementalBackupDurationInMs = incrementalBackupDurationInMs;
    }

    /**
     * @deprecated The property name did not match the server (IncrementalBackupDurationInMs), so it was never filled. Use {@link #getIncrementalBackupDurationInMs()} instead.
     * @return incremental backup duration in milliseconds
     */
    @Deprecated
    @JsonIgnore
    public Long getIncrementalBackupDurationIsMs() {
        return getIncrementalBackupDurationInMs();
    }

    /**
     * @deprecated The property name did not match the server (IncrementalBackupDurationInMs). Use {@link #setIncrementalBackupDurationInMs(Long)} instead.
     * @param incrementalBackupDurationIsMs incremental backup duration in milliseconds
     */
    @Deprecated
    @JsonIgnore
    public void setIncrementalBackupDurationIsMs(Long incrementalBackupDurationIsMs) {
        setIncrementalBackupDurationInMs(incrementalBackupDurationIsMs);
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
