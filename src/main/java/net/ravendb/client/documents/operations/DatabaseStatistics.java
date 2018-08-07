package net.ravendb.client.documents.operations;

import net.ravendb.client.util.Size;

import java.util.Arrays;
import java.util.Date;

public class DatabaseStatistics {

    private Long lastDocEtag;
    private int countOfIndexes;
    private long countOfDocuments;
    private long countOfRevisionDocuments;
    private long countOfDocumentsConflicts;
    private long countOfTombstones;
    private long countOfConflicts;
    private long countOfAttachments;
    private long countOfCounters;
    private long countOfUniqueAttachments;

    private IndexInformation[] indexes;

    private String databaseChangeVector;
    private String databaseId;
    private boolean is64Bit;
    private String pager;
    private Date lastIndexingTime;
    private Size sizeOnDisk;
    private Size tempBuffersSizeOnDisk;
    private int numberOfTransactionMergerQueueOperations;

    public IndexInformation[] getStaleIndexes() {
        return Arrays.stream(indexes)
            .filter(x -> x.isStale())
            .toArray(IndexInformation[]::new);
    }

    public IndexInformation[] getIndexes() {
        return indexes;
    }

    public void setIndexes(IndexInformation[] indexes) {
        this.indexes = indexes;
    }

    public Long getLastDocEtag() {
        return lastDocEtag;
    }

    public void setLastDocEtag(Long lastDocEtag) {
        this.lastDocEtag = lastDocEtag;
    }

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

    public long getCountOfUniqueAttachments() {
        return countOfUniqueAttachments;
    }

    public void setCountOfUniqueAttachments(long countOfUniqueAttachments) {
        this.countOfUniqueAttachments = countOfUniqueAttachments;
    }

    public String getDatabaseChangeVector() {
        return databaseChangeVector;
    }

    public void setDatabaseChangeVector(String databaseChangeVector) {
        this.databaseChangeVector = databaseChangeVector;
    }

    public String getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(String databaseId) {
        this.databaseId = databaseId;
    }

    public boolean isIs64Bit() {
        return is64Bit;
    }

    public void setIs64Bit(boolean is64Bit) {
        this.is64Bit = is64Bit;
    }

    public String getPager() {
        return pager;
    }

    public void setPager(String pager) {
        this.pager = pager;
    }

    public Date getLastIndexingTime() {
        return lastIndexingTime;
    }

    public void setLastIndexingTime(Date lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }

    public Size getTempBuffersSizeOnDisk() {
        return tempBuffersSizeOnDisk;
    }

    public void setTempBuffersSizeOnDisk(Size tempBuffersSizeOnDisk) {
        this.tempBuffersSizeOnDisk = tempBuffersSizeOnDisk;
    }

    public Size getSizeOnDisk() {
        return sizeOnDisk;
    }

    public void setSizeOnDisk(Size sizeOnDisk) {
        this.sizeOnDisk = sizeOnDisk;
    }

    public int getNumberOfTransactionMergerQueueOperations() {
        return numberOfTransactionMergerQueueOperations;
    }

    public void setNumberOfTransactionMergerQueueOperations(int numberOfTransactionMergerQueueOperations) {
        this.numberOfTransactionMergerQueueOperations = numberOfTransactionMergerQueueOperations;
    }

    public long getCountOfCounters() {
        return countOfCounters;
    }

    public void setCountOfCounters(long countOfCounters) {
        this.countOfCounters = countOfCounters;
    }
}
