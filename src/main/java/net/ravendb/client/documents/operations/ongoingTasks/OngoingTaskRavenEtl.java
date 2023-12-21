package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.RavenEtlConfiguration;

public class OngoingTaskRavenEtl extends OngoingTask {
    public OngoingTaskRavenEtl() {
        setTaskType(OngoingTaskType.RAVEN_ETL);
    }

    private String destinationUrl;
    private String destinationDatabase;
    private String connectionStringName;
    private String[] topologyDiscoveryUrls;

    private RavenEtlConfiguration configuration;

    public RavenEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(RavenEtlConfiguration configuration) {
        this.configuration = configuration;
    }

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

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String[] getTopologyDiscoveryUrls() {
        return topologyDiscoveryUrls;
    }

    public void setTopologyDiscoveryUrls(String[] topologyDiscoveryUrls) {
        this.topologyDiscoveryUrls = topologyDiscoveryUrls;
    }
}
