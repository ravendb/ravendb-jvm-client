package net.ravendb.client.documents.operations.cdcSink;

import java.util.ArrayList;
import java.util.List;

public class CdcSinkEmbeddedTableConfig {

    private String sourceTableSchema;
    private String sourceTableName;
    private String propertyName;
    private List<CdcColumnMapping> columns = new ArrayList<>();
    private List<String> primaryKeyColumns = new ArrayList<>();
    private List<String> joinColumns = new ArrayList<>();
    private CdcSinkRelationType type;
    private String patch;
    private CdcSinkOnDeleteConfig onDelete;
    private boolean caseSensitiveKeys;
    private List<CdcSinkEmbeddedTableConfig> embeddedTables = new ArrayList<>();
    private List<CdcSinkLinkedTableConfig> linkedTables = new ArrayList<>();

    /**
     * @return the SQL schema name
     */
    public String getSourceTableSchema() {
        return sourceTableSchema;
    }

    public void setSourceTableSchema(String sourceTableSchema) {
        this.sourceTableSchema = sourceTableSchema;
    }

    /**
     * @return the SQL table name
     */
    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    /**
     * @return the property name in the parent document (e.g. "Lines")
     */
    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * @return the column mappings defining how SQL columns are stored in the embedded object.
     *         Each entry maps a SQL column to a property or an attachment.
     */
    public List<CdcColumnMapping> getColumns() {
        return columns;
    }

    public void setColumns(List<CdcColumnMapping> columns) {
        this.columns = columns;
    }

    /**
     * @return the primary key columns of this embedded table, used for matching items within
     *         arrays/maps during updates and deletes
     */
    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public void setPrimaryKeyColumns(List<String> primaryKeyColumns) {
        this.primaryKeyColumns = primaryKeyColumns;
    }

    /**
     * @return the foreign key columns that join this table to its parent
     */
    public List<String> getJoinColumns() {
        return joinColumns;
    }

    public void setJoinColumns(List<String> joinColumns) {
        this.joinColumns = joinColumns;
    }

    /**
     * @return how the embedded data is stored: {@link CdcSinkRelationType#ARRAY} as a JSON array,
     *         {@link CdcSinkRelationType#MAP} as a JSON object keyed by PK, {@link CdcSinkRelationType#VALUE}
     *         as a single object
     */
    public CdcSinkRelationType getType() {
        return type;
    }

    public void setType(CdcSinkRelationType type) {
        this.type = type;
    }

    /**
     * Gets the optional JavaScript patch that runs on the PARENT document after this embedded operation
     * (i.e. after the embedded item has already been inserted/updated/removed in the array/map/value).
     *
     * <p>Available variables:</p>
     * <ul>
     *   <li>{@code this} = the parent document AFTER the embedded operation has been applied
     *       (the item is already inserted/updated/removed),</li>
     *   <li>{@code $row} = the raw CDC row for the embedded table with all columns as-is from the source database,</li>
     *   <li>{@code $old} = the embedded item as it existed BEFORE this CDC event modified it (null for inserts).</li>
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
     * @return how DELETE events are handled for this embedded table. When null (default), deletes remove the
     *         embedded item from the parent's array/map/value. See {@link CdcSinkOnDeleteConfig} for archive,
     *         audit, and ignore patterns.
     */
    public CdcSinkOnDeleteConfig getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(CdcSinkOnDeleteConfig onDelete) {
        this.onDelete = onDelete;
    }

    /**
     * @return whether primary key matching and map key comparison are case-sensitive. When false (default),
     *         string PK values and map keys are compared using ordinal case-insensitive comparison.
     *         When true, comparison is ordinal case-sensitive.
     */
    public boolean isCaseSensitiveKeys() {
        return caseSensitiveKeys;
    }

    public void setCaseSensitiveKeys(boolean caseSensitiveKeys) {
        this.caseSensitiveKeys = caseSensitiveKeys;
    }

    /**
     * @return the nested embedded tables (deep nesting). Requires that the nested table has a denormalized
     *         FK to the root table.
     */
    public List<CdcSinkEmbeddedTableConfig> getEmbeddedTables() {
        return embeddedTables;
    }

    public void setEmbeddedTables(List<CdcSinkEmbeddedTableConfig> embeddedTables) {
        this.embeddedTables = embeddedTables;
    }

    /**
     * @return the tables referenced by document ID link within this embedded table's items. Works identically
     *         to root-level linked tables: FK columns in the embedded row are resolved to document ID references
     *         in the target collection.
     */
    public List<CdcSinkLinkedTableConfig> getLinkedTables() {
        return linkedTables;
    }

    public void setLinkedTables(List<CdcSinkLinkedTableConfig> linkedTables) {
        this.linkedTables = linkedTables;
    }
}
