package net.ravendb.client.documents.operations;

import java.util.HashMap;
import java.util.Map;

public class CollectionStatistics {

    private int countOfDocuments;
    private int countOfConflicts;
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

    public int getCountOfDocuments() {
        return countOfDocuments;
    }

    public void setCountOfDocuments(int countOfDocuments) {
        this.countOfDocuments = countOfDocuments;
    }

    public int getCountOfConflicts() {
        return countOfConflicts;
    }

    public void setCountOfConflicts(int countOfConflicts) {
        this.countOfConflicts = countOfConflicts;
    }
}
