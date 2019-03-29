package net.ravendb.client.documents.operations.backups;

public class UploadProgress {
    private UploadType uploadType;
    private UploadState uploadState;
    private long uploadedInBytes;
    private long totalInBytes;
    private double bytesPutsPerSec;
    private long uploadTimeInMs;

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public UploadState getUploadState() {
        return uploadState;
    }

    public void setUploadState(UploadState uploadState) {
        this.uploadState = uploadState;
    }

    public long getUploadedInBytes() {
        return uploadedInBytes;
    }

    public void setUploadedInBytes(long uploadedInBytes) {
        this.uploadedInBytes = uploadedInBytes;
    }

    public long getTotalInBytes() {
        return totalInBytes;
    }

    public void setTotalInBytes(long totalInBytes) {
        this.totalInBytes = totalInBytes;
    }

    public double getBytesPutsPerSec() {
        return bytesPutsPerSec;
    }

    public void setBytesPutsPerSec(double bytesPutsPerSec) {
        this.bytesPutsPerSec = bytesPutsPerSec;
    }

    public long getUploadTimeInMs() {
        return uploadTimeInMs;
    }

    public void setUploadTimeInMs(long uploadTimeInMs) {
        this.uploadTimeInMs = uploadTimeInMs;
    }
}
