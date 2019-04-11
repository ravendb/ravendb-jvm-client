package net.ravendb.client.documents.operations.ongoingTasks;

import java.util.Date;

public class OngoingTaskSubscription extends OngoingTask {
    public OngoingTaskSubscription() {
        setTaskType(OngoingTaskType.SUBSCRIPTION);
    }

    private String query;
    private String subscriptionName;
    private long subscriptionId;
    private String mentorNode;
    private String ChangeVectorForNextBatchStartingPoint;
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

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public String getChangeVectorForNextBatchStartingPoint() {
        return ChangeVectorForNextBatchStartingPoint;
    }

    public void setChangeVectorForNextBatchStartingPoint(String changeVectorForNextBatchStartingPoint) {
        ChangeVectorForNextBatchStartingPoint = changeVectorForNextBatchStartingPoint;
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
