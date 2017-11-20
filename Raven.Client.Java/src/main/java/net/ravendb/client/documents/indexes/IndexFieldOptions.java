package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.indexes.spatial.SpatialOptions;

public class IndexFieldOptions {
    private FieldStorage storage;
    private FieldIndexing indexing;
    private FieldTermVector termVector;
    private SpatialOptions spatial;
    private String analyzer;
    private boolean suggestions;

    public FieldStorage getStorage() {
        return storage;
    }

    public void setStorage(FieldStorage storage) {
        this.storage = storage;
    }

    public FieldIndexing getIndexing() {
        return indexing;
    }

    public void setIndexing(FieldIndexing indexing) {
        this.indexing = indexing;
    }

    public FieldTermVector getTermVector() {
        return termVector;
    }

    public void setTermVector(FieldTermVector termVector) {
        this.termVector = termVector;
    }

    public SpatialOptions getSpatial() {
        return spatial;
    }

    public void setSpatial(SpatialOptions spatial) {
        this.spatial = spatial;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public boolean isSuggestions() {
        return suggestions;
    }

    public void setSuggestions(boolean suggestions) {
        this.suggestions = suggestions;
    }
}
