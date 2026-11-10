package net.ravendb.client.documents.operations.ongoingTasks;

import java.time.Duration;

public class OngoingTaskPullReplicationAsHub extends OngoingTask {
    public OngoingTaskPullReplicationAsHub() {
        setTaskType(OngoingTaskType.PULL_REPLICATION_AS_HUB);
    }

    private String destinationUrl;
    private String destinationDatabase;
    private Duration delayReplicationFor;
    private String fromToString;
    private String handlerId;
    private String lastAcceptedChangeVectorFromDestination;
    private String sourceDatabaseChangeVector;
    private long lastSentEtag;
    private long lastDatabaseEtag;

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    public String getDestinationDatabase() {
        return destinationDatabase;
    }

    public void setDestinationDatabase(String destinationDatabase) {
        this.destinationDatabase = destinationDatabase;
    }

    public Duration getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(Duration delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    public String getFromToString() {
        return fromToString;
    }

    public void setFromToString(String fromToString) {
        this.fromToString = fromToString;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getLastAcceptedChangeVectorFromDestination() {
        return lastAcceptedChangeVectorFromDestination;
    }

    public void setLastAcceptedChangeVectorFromDestination(String lastAcceptedChangeVectorFromDestination) {
        this.lastAcceptedChangeVectorFromDestination = lastAcceptedChangeVectorFromDestination;
    }

    public String getSourceDatabaseChangeVector() {
        return sourceDatabaseChangeVector;
    }

    public void setSourceDatabaseChangeVector(String sourceDatabaseChangeVector) {
        this.sourceDatabaseChangeVector = sourceDatabaseChangeVector;
    }

    public long getLastSentEtag() {
        return lastSentEtag;
    }

    public void setLastSentEtag(long lastSentEtag) {
        this.lastSentEtag = lastSentEtag;
    }

    public long getLastDatabaseEtag() {
        return lastDatabaseEtag;
    }

    public void setLastDatabaseEtag(long lastDatabaseEtag) {
        this.lastDatabaseEtag = lastDatabaseEtag;
    }
}
