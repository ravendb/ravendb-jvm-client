package net.ravendb.client.documents.indexes;

import net.ravendb.client.util.Size;

import java.util.Date;

public class IndexingPerformanceBasicStats {
    private long inputCount;
    private long failedCount;
    private long outputCount;
    private long successCount;
    private Date started;
    private double durationInMs;
    private Size allocatedManagedBytes;
    private Size allocatedUnmanagedBytes;
    private Size documentsSize;
    private long tombstoneDeleteSuccesses;

    public long getInputCount() {
        return inputCount;
    }

    public void setInputCount(long inputCount) {
        this.inputCount = inputCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public long getOutputCount() {
        return outputCount;
    }

    public void setOutputCount(long outputCount) {
        this.outputCount = outputCount;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public double getDurationInMs() {
        return durationInMs;
    }

    public void setDurationInMs(double durationInMs) {
        this.durationInMs = durationInMs;
    }

    public Size getAllocatedManagedBytes() {
        return allocatedManagedBytes;
    }

    public void setAllocatedManagedBytes(Size allocatedManagedBytes) {
        this.allocatedManagedBytes = allocatedManagedBytes;
    }

    public Size getAllocatedUnmanagedBytes() {
        return allocatedUnmanagedBytes;
    }

    public void setAllocatedUnmanagedBytes(Size allocatedUnmanagedBytes) {
        this.allocatedUnmanagedBytes = allocatedUnmanagedBytes;
    }

    public Size getDocumentsSize() {
        return documentsSize;
    }

    public void setDocumentsSize(Size documentsSize) {
        this.documentsSize = documentsSize;
    }

    public long getTombstoneDeleteSuccesses() {
        return tombstoneDeleteSuccesses;
    }

    public void setTombstoneDeleteSuccesses(long tombstoneDeleteSuccesses) {
        this.tombstoneDeleteSuccesses = tombstoneDeleteSuccesses;
    }
}
