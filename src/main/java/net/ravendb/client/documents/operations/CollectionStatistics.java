package net.ravendb.client.documents.operations;

import java.util.Map;
import java.util.TreeMap;

public class CollectionStatistics {

    private long countOfDocuments;
    private long countOfConflicts;
    private long countOfRevisionDocuments;
    private long countOfTombstones;
    private long countOfTimeSeriesDeletedRanges;
    private long countOfDocumentsConflicts;
    private long countOfAttachments;
    private long countOfCounterEntries;
    private long countOfTimeSeriesSegments;
    private Map<String, Long> collections;

    public CollectionStatistics() {
        collections = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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

    public long getCountOfRevisionDocuments() {
        return countOfRevisionDocuments;
    }

    public void setCountOfRevisionDocuments(long countOfRevisionDocuments) {
        this.countOfRevisionDocuments = countOfRevisionDocuments;
    }

    public long getCountOfTombstones() {
        return countOfTombstones;
    }

    public void setCountOfTombstones(long countOfTombstones) {
        this.countOfTombstones = countOfTombstones;
    }

    public long getCountOfTimeSeriesDeletedRanges() {
        return countOfTimeSeriesDeletedRanges;
    }

    public void setCountOfTimeSeriesDeletedRanges(long countOfTimeSeriesDeletedRanges) {
        this.countOfTimeSeriesDeletedRanges = countOfTimeSeriesDeletedRanges;
    }

    public long getCountOfDocumentsConflicts() {
        return countOfDocumentsConflicts;
    }

    public void setCountOfDocumentsConflicts(long countOfDocumentsConflicts) {
        this.countOfDocumentsConflicts = countOfDocumentsConflicts;
    }

    public long getCountOfAttachments() {
        return countOfAttachments;
    }

    public void setCountOfAttachments(long countOfAttachments) {
        this.countOfAttachments = countOfAttachments;
    }

    public long getCountOfCounterEntries() {
        return countOfCounterEntries;
    }

    public void setCountOfCounterEntries(long countOfCounterEntries) {
        this.countOfCounterEntries = countOfCounterEntries;
    }

    public long getCountOfTimeSeriesSegments() {
        return countOfTimeSeriesSegments;
    }

    public void setCountOfTimeSeriesSegments(long countOfTimeSeriesSegments) {
        this.countOfTimeSeriesSegments = countOfTimeSeriesSegments;
    }

    public Map<String, Long> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, Long> collections) {
        if (collections == null) {
            this.collections = null;
            return;
        }

        Map<String, Long> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.putAll(collections);
        this.collections = caseInsensitive;
    }
}
