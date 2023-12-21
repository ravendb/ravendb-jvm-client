package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;

public class OngoingTaskSqlEtl extends OngoingTask {

    public OngoingTaskSqlEtl() {
        setTaskType(OngoingTaskType.SQL_ETL);
    }

    private String destinationServer;
    private String destinationDatabase;
    private String connectionStringName;
    private boolean connectionStringDefined;
    private SqlEtlConfiguration configuration;

    public String getDestinationServer() {
        return destinationServer;
    }

    public void setDestinationServer(String destinationServer) {
        this.destinationServer = destinationServer;
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

    public boolean isConnectionStringDefined() {
        return connectionStringDefined;
    }

    public void setConnectionStringDefined(boolean connectionStringDefined) {
        this.connectionStringDefined = connectionStringDefined;
    }

    public SqlEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SqlEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
