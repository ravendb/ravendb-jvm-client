package net.ravendb.client.documents.operations.refresh;

public class RefreshConfiguration {

    private boolean disabled;
    private Long refreshFrequencyInSec;
    private Long maxItemsToProcess;

    public Long getMaxItemsToProcess() {
        return maxItemsToProcess;
    }

    public void setMaxItemsToProcess(Long maxItemsToProcess) {
        this.maxItemsToProcess = maxItemsToProcess;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Long getRefreshFrequencyInSec() {
        return refreshFrequencyInSec;
    }

    public void setRefreshFrequencyInSec(Long refreshFrequencyInSec) {
        this.refreshFrequencyInSec = refreshFrequencyInSec;
    }
}
