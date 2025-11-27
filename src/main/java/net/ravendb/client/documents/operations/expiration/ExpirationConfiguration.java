package net.ravendb.client.documents.operations.expiration;

public class ExpirationConfiguration {
    private boolean disabled;
    private Long deleteFrequencyInSec;
    private Long maxItemsToProcess;

    public Long getMaxItemsToProcess() { return maxItemsToProcess; }

    public void setMaxItemsToProcess(Long maxItemsToProcess) { this.maxItemsToProcess = maxItemsToProcess;}

    public Long getDeleteFrequencyInSec() {
        return deleteFrequencyInSec;
    }

    public void setDeleteFrequencyInSec(Long deleteFrequencyInSec) {
        this.deleteFrequencyInSec = deleteFrequencyInSec;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
