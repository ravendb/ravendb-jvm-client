package net.ravendb.client.documents.operations.ongoingTasks;

import java.util.Date;

public class OngoingTaskSubscription extends OngoingTask {
    public OngoingTaskSubscription() {
        setTaskType(OngoingTaskType.SUBSCRIPTION);
    }

    private String query;
    private String subscriptionName;
    private long subscriptionId;
    private String changeVectorForNextBatchStartingPoint;
    private Date lastBatchAckTime;
    private boolean disabled;
    private Date lastClientConnectionTime;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getChangeVectorForNextBatchStartingPoint() {
        return changeVectorForNextBatchStartingPoint;
    }

    public void setChangeVectorForNextBatchStartingPoint(String changeVectorForNextBatchStartingPoint) {
        this.changeVectorForNextBatchStartingPoint = changeVectorForNextBatchStartingPoint;
    }

    public Date getLastBatchAckTime() {
        return lastBatchAckTime;
    }

    public void setLastBatchAckTime(Date lastBatchAckTime) {
        this.lastBatchAckTime = lastBatchAckTime;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Date getLastClientConnectionTime() {
        return lastClientConnectionTime;
    }

    public void setLastClientConnectionTime(Date lastClientConnectionTime) {
        this.lastClientConnectionTime = lastClientConnectionTime;
    }
}
