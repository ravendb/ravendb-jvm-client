package net.ravendb.client.documents.operations.dataArchival;

public class DataArchivalConfiguration {
    private boolean disabled;
    private Long archiveFrequencyInSec;
    private Long maxItemsToProcess;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Long getArchiveFrequencyInSec() {
        return archiveFrequencyInSec;
    }

    public void setArchiveFrequencyInSec(Long archiveFrequencyInSec) {
        this.archiveFrequencyInSec = archiveFrequencyInSec;
    }

    public Long getMaxItemsToProcess() {
        return maxItemsToProcess;
    }

    public void setMaxItemsToProcess(Long maxItemsToProcess) {
        this.maxItemsToProcess = maxItemsToProcess;
    }
}
