package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.olap.OlapEtlConfiguration;

public class OngoingTaskOlapEtlDetails extends OngoingTask {

    public OngoingTaskOlapEtlDetails() {
        setTaskType(OngoingTaskType.OLAP_ETL);
    }

    private OlapEtlConfiguration configuration;

    public OlapEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(OlapEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
