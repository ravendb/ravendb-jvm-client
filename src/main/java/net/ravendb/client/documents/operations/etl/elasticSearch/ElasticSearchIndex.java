package net.ravendb.client.documents.operations.etl.elasticSearch;

public class ElasticSearchIndex {
    private String indexName;
    private String documentIdProperty;
    private boolean insertOnlyMode;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getDocumentIdProperty() {
        return documentIdProperty;
    }

    public void setDocumentIdProperty(String documentIdProperty) {
        this.documentIdProperty = documentIdProperty;
    }

    public boolean isInsertOnlyMode() {
        return insertOnlyMode;
    }

    public void setInsertOnlyMode(boolean insertOnlyMode) {
        this.insertOnlyMode = insertOnlyMode;
    }
}
