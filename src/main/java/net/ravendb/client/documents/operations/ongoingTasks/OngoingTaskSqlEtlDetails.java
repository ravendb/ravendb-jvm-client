package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.sql.SqlEtlConfiguration;

public class OngoingTaskSqlEtlDetails extends OngoingTask {

    public OngoingTaskSqlEtlDetails() {
        setTaskType(OngoingTaskType.SQL_ETL);
    }

    private SqlEtlConfiguration configuration;

    public SqlEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SqlEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
