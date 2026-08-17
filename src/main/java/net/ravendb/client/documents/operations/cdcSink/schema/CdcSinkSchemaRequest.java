package net.ravendb.client.documents.operations.cdcSink.schema;

import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;

/**
 * Body of {@code POST /admin/cdc-sink/schema}. The endpoint surfaces the source database's tables,
 * columns, PKs, and FKs annotated with CDC-specific hints so a mapping UI can be rendered before
 * saving a CDC task.
 */
public class CdcSinkSchemaRequest {

    private SqlConnectionString connection;
    private String connectionStringName;
    private String[] schemas;

    /**
     * @return the inline credentials. Required path when the connection is still being edited and hasn't
     *         been saved to {@code databaseRecord.SqlConnectionStrings} yet. When null, falls back to
     *         {@link #getConnectionStringName()}.
     */
    public SqlConnectionString getConnection() {
        return connection;
    }

    public void setConnection(SqlConnectionString connection) {
        this.connection = connection;
    }

    /**
     * @return the optional fallback for post-save callers. Ignored when {@link #getConnection()} is populated.
     */
    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    /**
     * Gets the provider-specific schema filter. Currently only consumed by PostgreSQL (defaults to
     * {@code ["public"]} when null/empty).
     *
     * <p>
     * Each entry is server-validated against the SQL identifier shape {@code ^[A-Za-z_][A-Za-z0-9_]*$}.
     * Legal-when-quoted Postgres schema names that contain hyphens, dots, or non-ASCII characters will be
     * rejected with a structured error.
     * </p>
     * @return the schema filter
     */
    public String[] getSchemas() {
        return schemas;
    }

    public void setSchemas(String[] schemas) {
        this.schemas = schemas;
    }
}
