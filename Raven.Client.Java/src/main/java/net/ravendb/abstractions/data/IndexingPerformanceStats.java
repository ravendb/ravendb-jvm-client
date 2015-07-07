package net.ravendb.abstractions.data;

import java.util.Date;

public class IndexingPerformanceStats {
    private String operation;
    private int itemsCount;
    private int inputCount;
    private int outputCount;
    private Date started;
    private Date completed;
    private String duration;
    private double durationMilliseconds;
    private int loadDocumentCount;
    private long loadDocumentDurationMs;
    private String waitingTimeSinceLastBatchCompleted;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }

    public int getInputCount() {
        return inputCount;
    }

    public void setInputCount(int inputCount) {
        this.inputCount = inputCount;
    }

    public int getOutputCount() {
        return outputCount;
    }

    public void setOutputCount(int outputCount) {
        this.outputCount = outputCount;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date getCompleted() {
        return completed;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public double getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public void setDurationMilliseconds(double durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
    }

    public int getLoadDocumentCount() {
        return loadDocumentCount;
    }

    public void setLoadDocumentCount(int loadDocumentCount) {
        this.loadDocumentCount = loadDocumentCount;
    }

    public long getLoadDocumentDurationMs() {
        return loadDocumentDurationMs;
    }

    public void setLoadDocumentDurationMs(long loadDocumentDurationMs) {
        this.loadDocumentDurationMs = loadDocumentDurationMs;
    }

    public String getWaitingTimeSinceLastBatchCompleted() {
        return waitingTimeSinceLastBatchCompleted;
    }

    public void setWaitingTimeSinceLastBatchCompleted(String waitingTimeSinceLastBatchCompleted) {
        this.waitingTimeSinceLastBatchCompleted = waitingTimeSinceLastBatchCompleted;
    }
}
