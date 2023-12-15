package net.ravendb.client.documents.operations;

import java.util.HashMap;
import java.util.Map;

public class CollectionStatistics {

    private long countOfDocuments;
    private long countOfConflicts;
    private Map<String, Long> collections;

    public CollectionStatistics() {
        collections = new HashMap<>();
    }

    public Map<String, Long> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, Long> collections) {
        this.collections = collections;
    }

    public long getCountOfDocuments() {
        return countOfDocuments;
    }

    public void setCountOfDocuments(long countOfDocuments) {
        this.countOfDocuments = countOfDocuments;
    }

    public long getCountOfConflicts() {
        return countOfConflicts;
    }

    public void setCountOfConflicts(long countOfConflicts) {
        this.countOfConflicts = countOfConflicts;
    }
}
