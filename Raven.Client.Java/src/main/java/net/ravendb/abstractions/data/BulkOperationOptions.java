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
   *  Determines whether operation details about each document should be returned by server.
   * @return
   */
  public boolean isRetrieveDetails() {
    return retrieveDetails;
  }

  /**
   *  Determines whether operation details about each document should be returned by server.
   * @param retrieveDetails
   */
  public void setRetrieveDetails(boolean retrieveDetails) {
    this.retrieveDetails = retrieveDetails;
  }

  /**
   * indicates whether operations are allowed on stale indexes.
   * @return
   */
  public boolean isAllowStale() {
    return allowStale;
  }

  /**
   * indicates whether operations are allowed on stale indexes.
   * @param allowStale
   */
  public void setAllowStale(boolean allowStale) {
    this.allowStale = allowStale;
  }

  public Long getStaleTimeout() {
    return staleTimeout;
  }

  public void setStaleTimeout(Long staleTimeout) {
    this.staleTimeout = staleTimeout;
  }

  /**
   * limits the amount of base operation per second allowed.
   * @return
   */
  public Integer getMaxOpsPerSec() {
    return maxOpsPerSec;
  }

  /**
   * limits the amount of base operation per second allowed.
   * @param maxOpsPerSec
   */
  public void setMaxOpsPerSec(Integer maxOpsPerSec) {
    this.maxOpsPerSec = maxOpsPerSec;
  }
}
