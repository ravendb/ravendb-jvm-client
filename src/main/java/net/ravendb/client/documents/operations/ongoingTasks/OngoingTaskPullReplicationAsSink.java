package net.ravendb.client.documents.operations.ongoingTasks;

public class OngoingTaskPullReplicationAsSink extends OngoingTask {

    private String hubDefinitionName;
    private String destinationUrl;
    private String[] topologyDiscoveryUrls;
    private String destinationDatabase;
    private String connectionStringName;
    private String certificatePublicKey;

    public OngoingTaskPullReplicationAsSink() {
        setTaskType(OngoingTaskType.PULL_REPLICATION_AS_SINK);
    }

    public String getHubDefinitionName() {
        return hubDefinitionName;
    }

    public void setHubDefinitionName(String hubDefinitionName) {
        this.hubDefinitionName = hubDefinitionName;
    }

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

    public String getCertificatePublicKey() {
        return certificatePublicKey;
    }

    public void setCertificatePublicKey(String certificatePublicKey) {
        this.certificatePublicKey = certificatePublicKey;
    }
}
