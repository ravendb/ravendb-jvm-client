package net.ravendb.abstractions.data;

import java.util.List;


public class VoronStorageStats {

  private long freePagesOverhead;
  private long rootPages;
  private long unallocatedPagesAtEndOfFile;
  private long usedDataFileSizeInBytes;
  private long allocatedDataFileSizeInBytes;
  private long nextWriteTransactionId;
  private List<VoronActiveTransaction> activeTransactions;

  public long getFreePagesOverhead() {
    return freePagesOverhead;
  }

  public void setFreePagesOverhead(long freePagesOverhead) {
    this.freePagesOverhead = freePagesOverhead;
  }

  public long getRootPages() {
    return rootPages;
  }

  public void setRootPages(long rootPages) {
    this.rootPages = rootPages;
  }

  public long getUnallocatedPagesAtEndOfFile() {
    return unallocatedPagesAtEndOfFile;
  }

  public void setUnallocatedPagesAtEndOfFile(long unallocatedPagesAtEndOfFile) {
    this.unallocatedPagesAtEndOfFile = unallocatedPagesAtEndOfFile;
  }

  public long getUsedDataFileSizeInBytes() {
    return usedDataFileSizeInBytes;
  }

  public void setUsedDataFileSizeInBytes(long usedDataFileSizeInBytes) {
    this.usedDataFileSizeInBytes = usedDataFileSizeInBytes;
  }

  public long getAllocatedDataFileSizeInBytes() {
    return allocatedDataFileSizeInBytes;
  }

  public void setAllocatedDataFileSizeInBytes(long allocatedDataFileSizeInBytes) {
    this.allocatedDataFileSizeInBytes = allocatedDataFileSizeInBytes;
  }

  public long getNextWriteTransactionId() {
    return nextWriteTransactionId;
  }

  public void setNextWriteTransactionId(long nextWriteTransactionId) {
    this.nextWriteTransactionId = nextWriteTransactionId;
  }

  public List<VoronActiveTransaction> getActiveTransactions() {
    return activeTransactions;
  }

  public void setActiveTransactions(List<VoronActiveTransaction> activeTransactions) {
    this.activeTransactions = activeTransactions;
  }

}
