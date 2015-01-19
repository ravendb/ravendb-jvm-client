package net.ravendb.abstractions.data;


public class SubscriptionBatchOptions {

  private Integer maxSize;
  private int maxDocCount;
  private Long acknowledgmentTimeout;

  public SubscriptionBatchOptions() {
    maxDocCount = 4096;
    acknowledgmentTimeout = 60 * 1000L;
  }

  public Integer getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(Integer maxSize) {
    this.maxSize = maxSize;
  }

  public int getMaxDocCount() {
    return maxDocCount;
  }

  public void setMaxDocCount(int maxDocCount) {
    this.maxDocCount = maxDocCount;
  }

  public Long getAcknowledgmentTimeout() {
    return acknowledgmentTimeout;
  }

  public void setAcknowledgmentTimeout(Long acknowledgmentTimeout) {
    this.acknowledgmentTimeout = acknowledgmentTimeout;
  }
}
