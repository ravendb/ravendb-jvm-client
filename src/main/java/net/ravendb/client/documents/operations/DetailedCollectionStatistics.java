package net.ravendb.client.documents.operations;

import java.util.Map;

public class DetailedCollectionStatistics {
    private long countOfDocuments;
    private long countOfConflicts;
    private Map<String, CollectionDetails> collections;

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

    public Map<String, CollectionDetails> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, CollectionDetails> collections) {
        this.collections = collections;
    }
}
