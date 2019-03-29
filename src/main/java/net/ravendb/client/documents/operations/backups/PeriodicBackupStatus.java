package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.serverwide.IDatabaseTaskStatus;

import java.util.Date;

public class PeriodicBackupStatus implements IDatabaseTaskStatus {

    private long taskId;
    private BackupType backupType;
    private boolean isFull;
    private String nodeTag;
    private Date lastFullBackup;
    private Date lastIncrementalBackup;
    private Date lastFullBackupInternal;
    private Date lastIncrementalBackupInternal;

    private LocalBackup localBackup;
    private UploadToS3 uploadToS3;
    private UploadToGlacier uploadToGlacier;
    private UploadToAzure uploadToAzure;
    private UploadToFtp uploadToFtp;

    private Long lastEtag;
    private LastRaftIndex lastRaftIndex;
    private String folderName;
    private Long durationInMs;
    private long version;
    private Error error;
    private Long lastOperationId;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

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

    public Date getLastFullBackupInternal() {
        return lastFullBackupInternal;
    }

    public void setLastFullBackupInternal(Date lastFullBackupInternal) {
        this.lastFullBackupInternal = lastFullBackupInternal;
    }

    public Date getLastIncrementalBackupInternal() {
        return lastIncrementalBackupInternal;
    }

    public void setLastIncrementalBackupInternal(Date lastIncrementalBackupInternal) {
        this.lastIncrementalBackupInternal = lastIncrementalBackupInternal;
    }

    public LocalBackup getLocalBackup() {
        return localBackup;
    }

    public void setLocalBackup(LocalBackup localBackup) {
        this.localBackup = localBackup;
    }

    public UploadToS3 getUploadToS3() {
        return uploadToS3;
    }

    public void setUploadToS3(UploadToS3 uploadToS3) {
        this.uploadToS3 = uploadToS3;
    }

    public UploadToGlacier getUploadToGlacier() {
        return uploadToGlacier;
    }

    public void setUploadToGlacier(UploadToGlacier uploadToGlacier) {
        this.uploadToGlacier = uploadToGlacier;
    }

    public UploadToAzure getUploadToAzure() {
        return uploadToAzure;
    }

    public void setUploadToAzure(UploadToAzure uploadToAzure) {
        this.uploadToAzure = uploadToAzure;
    }

    public UploadToFtp getUploadToFtp() {
        return uploadToFtp;
    }

    public void setUploadToFtp(UploadToFtp uploadToFtp) {
        this.uploadToFtp = uploadToFtp;
    }

    public Long getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(Long lastEtag) {
        this.lastEtag = lastEtag;
    }

    public LastRaftIndex getLastRaftIndex() {
        return lastRaftIndex;
    }

    public void setLastRaftIndex(LastRaftIndex lastRaftIndex) {
        this.lastRaftIndex = lastRaftIndex;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Long getDurationInMs() {
        return durationInMs;
    }

    public void setDurationInMs(Long durationInMs) {
        this.durationInMs = durationInMs;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public Long getLastOperationId() {
        return lastOperationId;
    }

    public void setLastOperationId(Long lastOperationId) {
        this.lastOperationId = lastOperationId;
    }

    public static class Error {
        private String exception;
        private Date at;

        public String getException() {
            return exception;
        }

        public void setException(String exception) {
            this.exception = exception;
        }

        public Date getAt() {
            return at;
        }

        public void setAt(Date at) {
            this.at = at;
        }
    }
}
