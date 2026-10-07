package net.ravendb.client.documents.operations.etl.snowflake;

public class SnowflakeEtlTable {
    private String tableName;
    private String documentIdColumn;
    private boolean insertOnlyMode;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDocumentIdColumn() {
        return documentIdColumn;
    }

    public void setDocumentIdColumn(String documentIdColumn) {
        this.documentIdColumn = documentIdColumn;
    }

    public boolean isInsertOnlyMode() {
        return insertOnlyMode;
    }

    public void setInsertOnlyMode(boolean insertOnlyMode) {
        this.insertOnlyMode = insertOnlyMode;
    }
}
