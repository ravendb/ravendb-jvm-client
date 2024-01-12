package net.ravendb.client.documents.operations;

import net.ravendb.client.util.Size;

import java.util.Arrays;
import java.util.Date;

public class DatabaseStatistics extends AbstractDatabaseStatistics<IndexInformation> {

    private Long lastDocEtag;
    private Long lastDatabaseEtag;
    private long countOfUniqueAttachments;
    private String databaseChangeVector;
    private String databaseId;
    private boolean is64Bit;
    private String pager;
    private Date lastIndexingTime;
    private Size sizeOnDisk;
    private Size tempBuffersSizeOnDisk;
    private int numberOfTransactionMergerQueueOperations;

    public IndexInformation[] getStaleIndexes() {
        return Arrays.stream(getIndexes())
            .filter(x -> x.isStale())
            .toArray(IndexInformation[]::new);
    }

    public Long getLastDocEtag() {
        return lastDocEtag;
    }

    public void setLastDocEtag(Long lastDocEtag) {
        this.lastDocEtag = lastDocEtag;
    }

    public Long getLastDatabaseEtag() {
        return lastDatabaseEtag;
    }

    public void setLastDatabaseEtag(Long lastDatabaseEtag) {
        this.lastDatabaseEtag = lastDatabaseEtag;
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

    /**
     * @return Storage engine component that handles the memory-mapped files
     */
    public String getPager() {
        return pager;
    }

    /**
     * @param pager Storage engine component that handles the memory-mapped files
     */
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

}
