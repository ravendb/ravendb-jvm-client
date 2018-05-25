package net.ravendb.client.documents.subscriptions;

import java.util.Date;

public class SubscriptionState {

    private String query;
    private String changeVectorForNextBatchStartingPoint;
    private long subscriptionId;
    private String subscriptionName;
    private String mentorNode;
    private String nodeTag;
    private Date lastBatchAckTime;
    private Date lastClientConnectionTime;
    private boolean disabled;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getChangeVectorForNextBatchStartingPoint() {
        return changeVectorForNextBatchStartingPoint;
    }

    public void setChangeVectorForNextBatchStartingPoint(String changeVectorForNextBatchStartingPoint) {
        this.changeVectorForNextBatchStartingPoint = changeVectorForNextBatchStartingPoint;
    }

    public long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public Date getLastBatchAckTime() {
        return lastBatchAckTime;
    }

    public void setLastBatchAckTime(Date lastBatchAckTime) {
        this.lastBatchAckTime = lastBatchAckTime;
    }

    public Date getLastClientConnectionTime() {
        return lastClientConnectionTime;
    }

    public void setLastClientConnectionTime(Date lastClientConnectionTime) {
        this.lastClientConnectionTime = lastClientConnectionTime;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
