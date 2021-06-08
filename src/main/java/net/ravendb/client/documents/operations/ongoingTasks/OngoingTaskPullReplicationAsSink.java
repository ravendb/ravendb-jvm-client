package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.replication.PullReplicationMode;

public class OngoingTaskPullReplicationAsSink extends OngoingTask {

    private String hubName;
    private PullReplicationMode mode;
    private String destinationUrl;
    private String[] topologyDiscoveryUrls;
    private String destinationDatabase;
    private String connectionStringName;
    private String certificatePublicKey;
    private String accessName;
    private String[] allowedHubToSinkPaths;
    private String[] allowedSinkToHubPaths;

    public OngoingTaskPullReplicationAsSink() {
        setTaskType(OngoingTaskType.PULL_REPLICATION_AS_SINK);
    }

    /**
     * HubDefinitionName is not supported anymore. Will be removed in next major version of the product. Use HubName instead.
     */
    public String getHubDefinitionName() {
        return hubName;
    }

    /**
     * HubDefinitionName is not supported anymore. Will be removed in next major version of the product. Use HubName instead.
     */
    public void setHubDefinitionName(String hubName) {
        this.hubName = hubName;
    }

    public String getHubName() {
        return hubName;
    }

    public void setHubName(String hubName) {
        this.hubName = hubName;
    }

    public PullReplicationMode getMode() {
        return mode;
    }

    public void setMode(PullReplicationMode mode) {
        this.mode = mode;
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

    public String getAccessName() {
        return accessName;
    }

    public void setAccessName(String accessName) {
        this.accessName = accessName;
    }

    public String[] getAllowedHubToSinkPaths() {
        return allowedHubToSinkPaths;
    }

    public void setAllowedHubToSinkPaths(String[] allowedHubToSinkPaths) {
        this.allowedHubToSinkPaths = allowedHubToSinkPaths;
    }

    public String[] getAllowedSinkToHubPaths() {
        return allowedSinkToHubPaths;
    }

    public void setAllowedSinkToHubPaths(String[] allowedSinkToHubPaths) {
        this.allowedSinkToHubPaths = allowedSinkToHubPaths;
    }
}
