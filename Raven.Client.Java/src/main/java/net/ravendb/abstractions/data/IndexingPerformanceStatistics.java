package net.ravendb.abstractions.data;

public class IndexingPerformanceStatistics {

    private int indexId;
    private String indexName;
    private IndexingPerformanceStats[] performance;

    public IndexingPerformanceStatistics() {
        performance = new IndexingPerformanceStats[0];
    }

    public int getIndexId() {
        return indexId;
    }

    public void setIndexId(int indexId) {
        this.indexId = indexId;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public IndexingPerformanceStats[] getPerformance() {
        return performance;
    }

    public void setPerformance(IndexingPerformanceStats[] performance) {
        this.performance = performance;
    }

}
