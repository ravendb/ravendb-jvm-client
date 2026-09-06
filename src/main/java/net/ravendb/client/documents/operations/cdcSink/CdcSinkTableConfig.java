package net.ravendb.client.documents.operations.cdcSink;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a single source SQL table to a RavenDB collection: column mappings,
 * primary key, optional transform patch, delete handling, and embedded/linked tables.
 */
public class CdcSinkTableConfig {

    private String collectionName;
    private String sourceTableSchema;
    private String sourceTableName;
    private List<CdcColumnMapping> columns = new ArrayList<>();
    private List<String> primaryKeyColumns = new ArrayList<>();
    private String patch;
    private CdcSinkOnDeleteConfig onDelete;
    private boolean disabled;
    private List<CdcSinkEmbeddedTableConfig> embeddedTables = new ArrayList<>();
    private List<CdcSinkLinkedTableConfig> linkedTables = new ArrayList<>();

    /**
     * @return the RavenDB collection name (e.g. "Orders")
     */
    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * @return the SQL schema name (e.g. "dbo", "public")
     */
    public String getSourceTableSchema() {
        return sourceTableSchema;
    }

    public void setSourceTableSchema(String sourceTableSchema) {
        this.sourceTableSchema = sourceTableSchema;
    }

    /**
     * @return the SQL table name (e.g. "orders")
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    /**
     * @return the column mappings defining how SQL columns are stored in the RavenDB document.
     *         Each entry maps a SQL column to a property or an attachment.
     */
    public List<CdcColumnMapping> getColumns() {
        return columns;
    }

    public void setColumns(List<CdcColumnMapping> columns) {
        this.columns = columns;
    }

    /**
     * @return the primary key column names, used for document ID generation
     */
    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
        this.primaryKeyColumns = primaryKeyColumns;
    }

    /**
     * Gets the optional JavaScript transformation patch. Runs on the document after column mapping and
     * embedded operations have been applied.
     *
     * <p>Available variables:</p>
     * <ul>
     *   <li>{@code this} = the document AFTER column mapping has been applied (already contains the new
     *       values from the CDC row),</li>
     *   <li>{@code $row} = the raw CDC row with all columns as-is from the source database,</li>
     *   <li>{@code $old} = the document as it was stored in RavenDB BEFORE this CDC event was processed
     *       (null for inserts).</li>
     * </ul>
     * @return the JavaScript patch, or null
     */
    public String getPatch() {
        return patch;
    }

    public void setPatch(String patch) {
        this.patch = patch;
    }

    /**
     * @return how DELETE events are handled for this table. When null (default), deletes are processed
     *         normally (the document is deleted). See {@link CdcSinkOnDeleteConfig} for archive, audit,
     *         and ignore patterns.
     */
    public CdcSinkOnDeleteConfig getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(CdcSinkOnDeleteConfig onDelete) {
        this.onDelete = onDelete;
    }

    /**
     * @return true when this table is skipped: no initial load and no change capture
     */
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return the tables embedded as nested objects/arrays within this collection's documents
     */
    public List<CdcSinkEmbeddedTableConfig> getEmbeddedTables() {
        return embeddedTables;
    }

    public void setEmbeddedTables(List<CdcSinkEmbeddedTableConfig> embeddedTables) {
        this.embeddedTables = embeddedTables;
    }

    /**
     * @return the tables referenced by document ID link within this collection's documents
     */
    public List<CdcSinkLinkedTableConfig> getLinkedTables() {
        return linkedTables;
    }

    public void setLinkedTables(List<CdcSinkLinkedTableConfig> linkedTables) {
        this.linkedTables = linkedTables;
    }
}
