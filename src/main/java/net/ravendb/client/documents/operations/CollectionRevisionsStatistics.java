package net.ravendb.client.documents.operations;

import java.util.Map;

public class CollectionRevisionsStatistics {
    private long countOfRevisions;
    private Map<String, Long> collections;

    public long getCountOfRevisions() {
        return countOfRevisions;
    }

    public void setCountOfRevisions(long countOfRevisions) {
        this.countOfRevisions = countOfRevisions;
    }

    public Map<String, Long> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, Long> collections) {
        this.collections = collections;
    }
}
