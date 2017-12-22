package net.ravendb.client.documents.queries;

import java.time.Duration;

/**
 * Holds different setting options for base operations.
 */
public class QueryOperationOptions {

    private Integer _maxOpsPerSecond;

    private boolean allowStale;

    private Duration staleTimeout;

    private boolean retrieveDetails;

    /**
     * Indicates whether operations are allowed on stale indexes.
     * @return true if stale result can be accepted
     */
    public boolean isAllowStale() {
        return allowStale;
    }

    /**
     * Indicates whether operations are allowed on stale indexes.
     * @param allowStale sets the value
     */
    public void setAllowStale(boolean allowStale) {
        this.allowStale = allowStale;
    }

    /**
     * If AllowStale is set to false and index is stale, then this is the maximum timeout to wait for index to become non-stale. If timeout is exceeded then exception is thrown.
     * @return max time server can wait for stale results
     */
    public Duration getStaleTimeout() {
        return staleTimeout;
    }

    /**
     * If AllowStale is set to false and index is stale, then this is the maximum timeout to wait for index to become non-stale. If timeout is exceeded then exception is thrown.
     * @param staleTimeout Sets the value
     */
    public void setStaleTimeout(Duration staleTimeout) {
        this.staleTimeout = staleTimeout;
    }

    /**
     * Limits the amount of base operation per second allowed.
     * @return maximum operations per seconds
     */
    public Integer getMaxOpsPerSecond() {
        return _maxOpsPerSecond;
    }

    /**
     * Limits the amount of base operation per second allowed.
     * @param maxOpsPerSecond sets the value
     */
    public void setMaxOpsPerSecond(Integer maxOpsPerSecond) {
        if (maxOpsPerSecond != null && maxOpsPerSecond <= 0) {
            throw new IllegalStateException("MaxOpsPerSecond must be greater than 0");
        }
        this._maxOpsPerSecond = maxOpsPerSecond;
    }

    /**
     * Determines whether operation details about each document should be returned by server.
     * @return true if operation details should be returned
     */
    public boolean isRetrieveDetails() {
        return retrieveDetails;
    }

    /**
     * Determines whether operation details about each document should be returned by server.
     * @param retrieveDetails Sets the value
     */
    public void setRetrieveDetails(boolean retrieveDetails) {
        this.retrieveDetails = retrieveDetails;
    }

}
