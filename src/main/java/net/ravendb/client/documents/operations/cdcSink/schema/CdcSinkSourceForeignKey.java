package net.ravendb.client.documents.operations.cdcSink.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreign-key reference from one source table to another, surfaced by the CDC schema-discovery
 * endpoint so callers can suggest linked tables for a CDC mapping.
 */
public class CdcSinkSourceForeignKey {

    private List<String> columns = new ArrayList<>();
    private String referencedSchema;
    private String referencedTable;
    private List<String> referencedColumns = new ArrayList<>();

    /**
     * @return the FK column(s) on the source ("child") table
     */
    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    /**
     * @return the schema of the referenced ("parent") table
     */
    public String getReferencedSchema() {
        return referencedSchema;
    }

    public void setReferencedSchema(String referencedSchema) {
        this.referencedSchema = referencedSchema;
    }

    /**
     * @return the name of the referenced ("parent") table
     */
    public String getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    /**
     * @return the PK column(s) on the referenced table
     */
    public List<String> getReferencedColumns() {
        return referencedColumns;
    }

    public void setReferencedColumns(List<String> referencedColumns) {
        this.referencedColumns = referencedColumns;
    }
}
