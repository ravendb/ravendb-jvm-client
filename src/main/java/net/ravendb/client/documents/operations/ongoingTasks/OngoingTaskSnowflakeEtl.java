package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.snowflake.SnowflakeEtlConfiguration;

public class OngoingTaskSnowflakeEtl extends OngoingTask {

    public OngoingTaskSnowflakeEtl() {
        setTaskType(OngoingTaskType.SNOWFLAKE_ETL);
    }

    private String connectionStringName;
    private String connectionString;
    private SnowflakeEtlConfiguration configuration;

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public SnowflakeEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SnowflakeEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
