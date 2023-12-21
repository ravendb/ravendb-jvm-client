package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.olap.OlapEtlConfiguration;

public class OngoingTaskOlapEtl extends OngoingTask {

    public OngoingTaskOlapEtl() {
        setTaskType(OngoingTaskType.OLAP_ETL);
    }

    private String connectionStringName;
    private String destination;
    private OlapEtlConfiguration configuration;

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public OlapEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(OlapEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
