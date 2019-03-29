package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.RavenEtlConfiguration;

public class OngoingTaskRavenEtlDetails extends OngoingTask {
    public OngoingTaskRavenEtlDetails() {
        setTaskType(OngoingTaskType.RAVEN_ETL);
    }

    private String destinationUrl;
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
}
