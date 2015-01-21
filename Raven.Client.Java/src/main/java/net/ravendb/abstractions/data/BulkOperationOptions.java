package net.ravendb.abstractions.data;

/**
 * Holds different settings options for base operations.
 */
public class BulkOperationOptions {
  private boolean allowStale;
  private Long staleTimeout;
  private Integer maxOpsPerSec;
  private boolean retrieveDetails;

  /**
   * Determines whether operation details about each document should be returned by server.
   */
  public boolean isRetrieveDetails() {
    return retrieveDetails;
  }

  /**
   * Determines whether operation details about each document should be returned by server.
   * @param retrieveDetails
   */
  public void setRetrieveDetails(boolean retrieveDetails) {
    this.retrieveDetails = retrieveDetails;
  }

  /**
   * Indicates whether operations are allowed on stale indexes.
   */
  public boolean isAllowStale() {
    return allowStale;
  }

  /**
   * Indicates whether operations are allowed on stale indexes.
   * @param allowStale
   */
  public void setAllowStale(boolean allowStale) {
    this.allowStale = allowStale;
  }

  /**
   * If AllowStale is set to false and index is stale, then this is the maximum timeout to wait for index to become non-stale. If timeout is exceeded then exception is thrown.
   * Value:
   * null by default - throw immediately if index is stale
   * {@value null by default - throw immediately if index is stale}
   */
  public Long getStaleTimeout() {
    return staleTimeout;
  }

  /**
   * If AllowStale is set to false and index is stale, then this is the maximum timeout to wait for index to become non-stale. If timeout is exceeded then exception is thrown.
   * Value:
   * null by default - throw immediately if index is stale
   * {@value null by default - throw immediately if index is stale}
   * @param staleTimeout
   */
  public void setStaleTimeout(Long staleTimeout) {
    this.staleTimeout = staleTimeout;
  }

  /**
   * Limits the amount of base operation per second allowed.
   */
  public Integer getMaxOpsPerSec() {
    return maxOpsPerSec;
  }

  /**
   * Limits the amount of base operation per second allowed.
   * @param maxOpsPerSec
   */
  public void setMaxOpsPerSec(Integer maxOpsPerSec) {
    this.maxOpsPerSec = maxOpsPerSec;
  }
}
