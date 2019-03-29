package net.ravendb.client.documents.operations.backups;

public abstract class CloudUploadStatus extends BackupStatus {
    private UploadProgress uploadProgress;
    private boolean skipped;

    public UploadProgress getUploadProgress() {
        return uploadProgress;
    }

    public void setUploadProgress(UploadProgress uploadProgress) {
        this.uploadProgress = uploadProgress;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
}
