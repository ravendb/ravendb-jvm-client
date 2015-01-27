package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.utils.TimeSpan;


public class SubscriptionBatchOptions {

  private Integer maxSize;
  private int maxDocCount;
  private Long acknowledgmentTimeout;

  @SuppressWarnings("boxing")
  public SubscriptionBatchOptions() {
    maxDocCount = 4096;
    acknowledgmentTimeout = 60 * 1000L;
  }

  public SubscriptionBatchOptions(Integer maxSize, int maxDocCount, Long acknowledgmentTimeout) {
    super();
    this.maxSize = maxSize;
    this.maxDocCount = maxDocCount;
    this.acknowledgmentTimeout = acknowledgmentTimeout;
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

  @SuppressWarnings("boxing")
  public RavenJObject toRavenObject() {
    RavenJObject result = new RavenJObject();
    result.add("MaxDocCount", maxDocCount);
    result.add("MaxSize", maxSize);
    result.add("AcknowledgmentTimeout", TimeSpan.formatString(acknowledgmentTimeout));
    return result;
  }
}
