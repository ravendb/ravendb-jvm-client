package net.ravendb.client.documents.operations;

public class BulkInsertProgress {
    private long total;
    private long batchCount;
    private String lastProcessedId;

    private long documentsProcessed;
    private long attachmentsProcessed;
    private long countersProcessed;
    private long timeSeriesProcessed;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(long batchCount) {
        this.batchCount = batchCount;
    }

    public String getLastProcessedId() {
        return lastProcessedId;
    }

    public void setLastProcessedId(String lastProcessedId) {
        this.lastProcessedId = lastProcessedId;
    }

    public long getDocumentsProcessed() {
        return documentsProcessed;
    }

    public void setDocumentsProcessed(long documentsProcessed) {
        this.documentsProcessed = documentsProcessed;
    }

    public long getAttachmentsProcessed() {
        return attachmentsProcessed;
    }

    public void setAttachmentsProcessed(long attachmentsProcessed) {
        this.attachmentsProcessed = attachmentsProcessed;
    }

    public long getCountersProcessed() {
        return countersProcessed;
    }

    public void setCountersProcessed(long countersProcessed) {
        this.countersProcessed = countersProcessed;
    }

    public long getTimeSeriesProcessed() {
        return timeSeriesProcessed;
    }

    public void setTimeSeriesProcessed(long timeSeriesProcessed) {
        this.timeSeriesProcessed = timeSeriesProcessed;
    }
}
