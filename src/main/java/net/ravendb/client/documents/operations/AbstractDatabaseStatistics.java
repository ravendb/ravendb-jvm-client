package net.ravendb.client.documents.operations;

public class AbstractDatabaseStatistics<TIndexInformation extends EssentialIndexInformation> {

    private int countOfIndexes;
    private long countOfDocuments;
    private long countOfRevisionDocuments;
    private long countOfDocumentsConflicts;
    private long countOfTombstones;
    private long countOfConflicts;
    private long countOfAttachments;
    private long countOfCounterEntries;
    private long countOfTimeSeriesSegments;
    private TIndexInformation[] indexes;

    public int getCountOfIndexes() {
        return countOfIndexes;
    }

    public void setCountOfIndexes(int countOfIndexes) {
        this.countOfIndexes = countOfIndexes;
    }

    public long getCountOfDocuments() {
        return countOfDocuments;
    }

    public void setCountOfDocuments(long countOfDocuments) {
        this.countOfDocuments = countOfDocuments;
    }

    public long getCountOfRevisionDocuments() {
        return countOfRevisionDocuments;
    }

    public void setCountOfRevisionDocuments(long countOfRevisionDocuments) {
        this.countOfRevisionDocuments = countOfRevisionDocuments;
    }

    public long getCountOfDocumentsConflicts() {
        return countOfDocumentsConflicts;
    }

    public void setCountOfDocumentsConflicts(long countOfDocumentsConflicts) {
        this.countOfDocumentsConflicts = countOfDocumentsConflicts;
    }

    public long getCountOfTombstones() {
        return countOfTombstones;
    }

    public void setCountOfTombstones(long countOfTombstones) {
        this.countOfTombstones = countOfTombstones;
    }

    public long getCountOfConflicts() {
        return countOfConflicts;
    }

    public void setCountOfConflicts(long countOfConflicts) {
        this.countOfConflicts = countOfConflicts;
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

    public TIndexInformation[] getIndexes() {
        return indexes;
    }

    public void setIndexes(TIndexInformation[] indexes) {
        this.indexes = indexes;
    }
}
