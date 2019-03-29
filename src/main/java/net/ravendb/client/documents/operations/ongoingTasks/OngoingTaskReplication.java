package net.ravendb.client.documents.operations.ongoingTasks;

public class OngoingTaskReplication extends OngoingTask {

    public OngoingTaskReplication() {
        setTaskType(OngoingTaskType.REPLICATION);
    }

    private String destinationUrl;
    private String[] topologyDiscoveryUrls;
    private String destinationDatabase;
    private String mentorNode;
    private String connectionStringName;
    private String delayReplicationFor;

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

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(String delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }
}
