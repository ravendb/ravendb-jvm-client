package net.ravendb.client.documents.smuggler;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum DatabaseRecordItemType {
    NONE,
    CONFLICT_SOLVER_CONFIG,
    SETTINGS,
    REVISIONS,
    EXPIRATION,
    PERIODIC_BACKUPS,
    EXTERNAL_REPLICATIONS,
    RAVEN_CONNECTION_STRINGS,
    SQL_CONNECTION_STRINGS,
    RAVEN_ETLS,
    SQL_ETLS,
    CLIENT,
    SORTERS,
    SINK_PULL_REPLICATIONS,
    HUB_PULL_REPLICATIONS
}
