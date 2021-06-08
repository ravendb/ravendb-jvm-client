package net.ravendb.client.documents.operations.etl.olap;

public class OlapEtlTable {

    private String tableName;
    private String documentIdColumn;

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
}
