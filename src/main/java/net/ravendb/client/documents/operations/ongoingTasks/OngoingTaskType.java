package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum OngoingTaskType {
    REPLICATION,
    RAVEN_ETL,
    SQL_ETL,
    BACKUP,
    SUBSCRIPTION
}
