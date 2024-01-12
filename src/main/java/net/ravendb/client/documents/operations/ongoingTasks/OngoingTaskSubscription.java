package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;

import java.util.Date;
import java.util.Map;

public class OngoingTaskSubscription extends OngoingTask {
    public OngoingTaskSubscription() {
        setTaskType(OngoingTaskType.SUBSCRIPTION);
    }

    private String query;
    private String subscriptionName;
    private long subscriptionId;
    private String changeVectorForNextBatchStartingPoint;
    private Map<String, String> changeVectorForNextBatchStartingPointPerShard;
    private ArchivedDataProcessingBehavior archivedDataProcessingBehavior;
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

    public Map<String, String> getChangeVectorForNextBatchStartingPointPerShard() {
        return changeVectorForNextBatchStartingPointPerShard;
    }

    public void setChangeVectorForNextBatchStartingPointPerShard(Map<String, String> changeVectorForNextBatchStartingPointPerShard) {
        this.changeVectorForNextBatchStartingPointPerShard = changeVectorForNextBatchStartingPointPerShard;
    }

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
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
