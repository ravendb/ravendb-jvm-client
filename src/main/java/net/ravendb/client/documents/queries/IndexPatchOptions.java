package net.ravendb.client.documents.queries;

import java.time.Duration;

/**
 * Options for waiting for indexing to complete after a patch operation.
 */
public class IndexPatchOptions {

    private Duration waitForIndexesTimeout = Duration.ofSeconds(15);
    private boolean throwOnTimeoutInWaitForIndexes;
    private String[] waitForSpecificIndexes;

    public IndexPatchOptions() {
    }

    public IndexPatchOptions(Duration waitForIndexesTimeout) {
        this.waitForIndexesTimeout = waitForIndexesTimeout;
    }

    /**
     * @return Maximum time to wait for the indexes to become non-stale.
     */
    public Duration getWaitForIndexesTimeout() {
        return waitForIndexesTimeout;
    }

    /**
     * @param waitForIndexesTimeout Maximum time to wait for the indexes to become non-stale.
     */
    public void setWaitForIndexesTimeout(Duration waitForIndexesTimeout) {
        this.waitForIndexesTimeout = waitForIndexesTimeout;
    }

    /**
     * @return if true, throw when the timeout is reached, otherwise return normally.
     */
    public boolean isThrowOnTimeoutInWaitForIndexes() {
        return throwOnTimeoutInWaitForIndexes;
    }

    /**
     * @param throwOnTimeoutInWaitForIndexes if true, throw when the timeout is reached, otherwise return normally.
     */
    public void setThrowOnTimeoutInWaitForIndexes(boolean throwOnTimeoutInWaitForIndexes) {
        this.throwOnTimeoutInWaitForIndexes = throwOnTimeoutInWaitForIndexes;
    }

    /**
     * A list of index names to wait for. When not set, the server waits only for the indexes it infers from the
     * documents matched by the query or patch. When provided, the server waits for exactly these index names.
     * @return index names
     */
    public String[] getWaitForSpecificIndexes() {
        return waitForSpecificIndexes;
    }

    /**
     * A list of index names to wait for. When not set, the server waits only for the indexes it infers from the
     * documents matched by the query or patch. When provided, the server waits for exactly these index names.
     * @param waitForSpecificIndexes index names
     */
    public void setWaitForSpecificIndexes(String[] waitForSpecificIndexes) {
        this.waitForSpecificIndexes = waitForSpecificIndexes;
    }
}
