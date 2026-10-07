package net.ravendb.client.documents.operations.cdcSink.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.operations.cdcSink.CdcSinkLinkedTableConfig;
import net.ravendb.client.documents.operations.cdcSink.CdcSinkTableConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * One source-side table as the CDC schema-discovery endpoint sees it. Field names mirror
 * {@link CdcSinkTableConfig} so a CDC mapping can be populated with minimal transformation.
 */
public class CdcSinkSourceTable {

    private String sourceTableSchema;
    private String sourceTableName;
    private List<CdcSinkSourceColumn> columns = new ArrayList<>();
    private List<String> primaryKeyColumns = new ArrayList<>();
    private List<CdcSinkSourceForeignKey> foreignKeys = new ArrayList<>();

    @JsonProperty("IsCdcEnabled")
    private boolean cdcEnabled;

    private String unsupportedReason;
    private List<String> warnings = new ArrayList<>();

    /**
     * @return the SQL schema. Matches {@link CdcSinkTableConfig#getSourceTableSchema()}.
     */
    public String getSourceTableSchema() {
        return sourceTableSchema;
    }

    public void setSourceTableSchema(String sourceTableSchema) {
        this.sourceTableSchema = sourceTableSchema;
    }

    /**
     * @return the SQL table name. Matches {@link CdcSinkTableConfig#getSourceTableName()}.
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    /**
     * @return the source columns of this table, in discovery order
     */
    public List<CdcSinkSourceColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<CdcSinkSourceColumn> columns) {
        this.columns = columns;
    }

    /**
     * @return the names of the columns that make up this table's primary key
     */
    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
        this.primaryKeyColumns = primaryKeyColumns;
    }

    /**
     * @return the foreign keys leaving this table, usable to suggest {@link CdcSinkLinkedTableConfig} entries
     */
    public List<CdcSinkSourceForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<CdcSinkSourceForeignKey> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }

    /**
     * @return true when CDC tracking is active for this table at the source. SQL Server: present in
     *         {@code cdc.change_tables}. PostgreSQL / MySQL: always true (publication / binlog membership
     *         is a database-level concern; surfaced elsewhere).
     */
    public boolean isCdcEnabled() {
        return cdcEnabled;
    }

    public void setCdcEnabled(boolean cdcEnabled) {
        this.cdcEnabled = cdcEnabled;
    }

    /**
     * @return the reason the entire table cannot be CDC-captured; null on usable tables
     */
    public String getUnsupportedReason() {
        return unsupportedReason;
    }

    public void setUnsupportedReason(String unsupportedReason) {
        this.unsupportedReason = unsupportedReason;
    }

    /**
     * @return non-fatal, table-scoped verification findings that don't make the table unusable but affect
     *         captured data quality — e.g. a PostgreSQL REPLICA IDENTITY that won't carry row-identifying
     *         columns on DELETE
     */
    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
