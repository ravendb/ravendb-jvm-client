package net.ravendb.client.documents.commands.batches;

import java.time.Duration;

public class IndexBatchOptions {

    private boolean waitForIndexes;
    private Duration waitForIndexesTimeout;
    private boolean throwOnTimeoutInWaitForIndexes;
    private String[] waitForSpecificIndexes;

    public boolean isWaitForIndexes() {
        return waitForIndexes;
    }

    public void setWaitForIndexes(boolean waitForIndexes) {
        this.waitForIndexes = waitForIndexes;
    }

    public Duration getWaitForIndexesTimeout() {
        return waitForIndexesTimeout;
    }

    public void setWaitForIndexesTimeout(Duration waitForIndexesTimeout) {
        this.waitForIndexesTimeout = waitForIndexesTimeout;
    }

    public boolean isThrowOnTimeoutInWaitForIndexes() {
        return throwOnTimeoutInWaitForIndexes;
    }

    public void setThrowOnTimeoutInWaitForIndexes(boolean throwOnTimeoutInWaitForIndexes) {
        this.throwOnTimeoutInWaitForIndexes = throwOnTimeoutInWaitForIndexes;
    }

    public String[] getWaitForSpecificIndexes() {
        return waitForSpecificIndexes;
    }

    public void setWaitForSpecificIndexes(String[] waitForSpecificIndexes) {
        this.waitForSpecificIndexes = waitForSpecificIndexes;
    }
}
