package net.ravendb.abstractions.data;

import java.util.Date;


public class SubscriptionConfig {

  private Long subscriptionId;
  private SubscriptionCriteria criteria;
  private Etag ackEtag;
  private Date timeOfSendingLastBatch;
  private Date TimeOfLastClientActivity;

  public Long getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(Long subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public SubscriptionCriteria getCriteria() {
    return criteria;
  }

  public void setCriteria(SubscriptionCriteria criteria) {
    this.criteria = criteria;
  }

  public Etag getAckEtag() {
    return ackEtag;
  }

  public void setAckEtag(Etag ackEtag) {
    this.ackEtag = ackEtag;
  }

  public Date getTimeOfSendingLastBatch() {
    return timeOfSendingLastBatch;
  }

  public void setTimeOfSendingLastBatch(Date timeOfSendingLastBatch) {
    this.timeOfSendingLastBatch = timeOfSendingLastBatch;
  }

  public Date getTimeOfLastClientActivity() {
    return TimeOfLastClientActivity;
  }

  public void setTimeOfLastClientActivity(Date timeOfLastClientActivity) {
    TimeOfLastClientActivity = timeOfLastClientActivity;
  }

}
