package net.ravendb.client.documents.indexes;

public class PutIndexResult {
    private String indexName;
    private int indexId;

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public int getIndexId() {
        return indexId;
    }

    public void setIndexId(int indexId) {
        this.indexId = indexId;
    }
}
