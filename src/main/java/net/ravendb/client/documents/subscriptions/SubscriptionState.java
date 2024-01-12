package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;

import java.util.Date;

public class SubscriptionState {

    private String query;
    private String changeVectorForNextBatchStartingPoint;
    private long subscriptionId;
    private String subscriptionName;
    private String mentorNode;

    private boolean pinToMentorNode;
    private String nodeTag;
    private Date lastBatchAckTime;
    private Date lastClientConnectionTime;
    private boolean disabled;
    private Long raftCommandIndex;

    private ArchivedDataProcessingBehavior archivedDataProcessingBehavior;
    private SubscriptionShardingState shardingState;

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

    public boolean isPinToMentorNode() {
        return pinToMentorNode;
    }

    public void setPinToMentorNode(boolean pinToMentorNode) {
        this.pinToMentorNode = pinToMentorNode;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
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

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
    }

    public SubscriptionShardingState getShardingState() {
        return shardingState;
    }

    public void setShardingState(SubscriptionShardingState shardingState) {
        this.shardingState = shardingState;
    }
}
