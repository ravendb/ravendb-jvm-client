package net.ravendb.client.documents.operations.ongoingTasks;

import java.time.Duration;

public class OngoingTaskReplication extends OngoingTask {

    public OngoingTaskReplication() {
        setTaskType(OngoingTaskType.REPLICATION);
    }

    private String destinationUrl;
    private String[] topologyDiscoveryUrls;
    private String destinationDatabase;
    private String connectionStringName;
    private Duration delayReplicationFor;
    private String fromToString;
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

    public String[] getTopologyDiscoveryUrls() {
        return topologyDiscoveryUrls;
    }

    public void setTopologyDiscoveryUrls(String[] topologyDiscoveryUrls) {
        this.topologyDiscoveryUrls = topologyDiscoveryUrls;
    }

    public String getDestinationDatabase() {
        return destinationDatabase;
    }

    public void setDestinationDatabase(String destinationDatabase) {
        this.destinationDatabase = destinationDatabase;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
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
