package net.ravendb.client.documents.operations.etl.sql;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum SqlProvider {
    SQL_CLIENT,
    NPGSQL,
    OLE_DB,
    ORACLE_CLIENT,
    MY_SQL_CLIENT,
    MY_SQL_CONNECTOR_FACTORY
}
