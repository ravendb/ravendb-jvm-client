package net.ravendb.client.documents.operations.ongoingTasks;

import java.time.Duration;

public class OngoingTaskPullReplicationAsHub extends OngoingTask {
    public OngoingTaskPullReplicationAsHub() {
        setTaskType(OngoingTaskType.PULL_REPLICATION_AS_HUB);
    }

    private String destinationUrl;
    private String destinationDatabase;
    private Duration delayReplicationFor;

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
}
