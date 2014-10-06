package net.ravendb.abstractions.data;

import java.util.Collection;
import java.util.UUID;

public class DatabaseStatistics {

  public static class TriggerInfo {
    private String type;
    private String name;

    public String getName() {
      return name;
    }
    public String getType() {
      return type;
    }
    public void setName(String name) {
      this.name = name;
    }
    public void setType(String type) {
      this.type = type;
    }
  }

  private Etag lastDocEtag;

  @Deprecated
  private Etag lastAttachmentEtag;
  private int countOfIndexes;
  private int countOfResultTransformers;
  private int inMemoryIndexingQueueSize;
  private long approximateTaskCount;
  private long countOfDocuments;

  @Deprecated
  private long countOfAttachments;
  private String[] staleIndexes;
  private int currentNumberOfItemsToIndexInSingleBatch;
  private int currentNumberOfItemsToReduceInSingleBatch;
  private float databaseTransactionVersionSizeInMB;
  private IndexStats[] indexes;
  private IndexingError[] errors;
  private IndexingBatchInfo[] indexingBatchInfo;
  private FutureBatchStats[] prefetches;
  private UUID databaseId;
  private boolean supportsDtc;

  public int getCountOfResultTransformers() {
    return countOfResultTransformers;
  }

  public void setCountOfResultTransformers(int countOfResultTransformers) {
    this.countOfResultTransformers = countOfResultTransformers;
  }

  public IndexingBatchInfo[] getIndexingBatchInfo() {
    return indexingBatchInfo;
  }

  public void setIndexingBatchInfo(IndexingBatchInfo[] indexingBatchInfo) {
    this.indexingBatchInfo = indexingBatchInfo;
  }

  public boolean isSupportsDtc() {
    return supportsDtc;
  }

  public void setSupportsDtc(boolean supportsDtc) {
    this.supportsDtc = supportsDtc;
  }
  public long getApproximateTaskCount() {
    return approximateTaskCount;
  }

  @Deprecated
  public long getCountOfAttachments() {
    return countOfAttachments;
  }

  @Deprecated
  public void setCountOfAttachments(long countOfAttachments) {
    this.countOfAttachments = countOfAttachments;
  }
  public long getCountOfDocuments() {
    return countOfDocuments;
  }
  public int getCountOfIndexes() {
    return countOfIndexes;
  }
  public int getCurrentNumberOfItemsToIndexInSingleBatch() {
    return currentNumberOfItemsToIndexInSingleBatch;
  }

  public int getCurrentNumberOfItemsToReduceInSingleBatch() {
    return currentNumberOfItemsToReduceInSingleBatch;
  }
  public UUID getDatabaseId() {
    return databaseId;
  }
  public float getDatabaseTransactionVersionSizeInMB() {
    return databaseTransactionVersionSizeInMB;
  }
  public IndexingError[] getErrors() {
    return errors;
  }
  public IndexStats[] getIndexes() {
    return indexes;
  }
  public int getInMemoryIndexingQueueSize() {
    return inMemoryIndexingQueueSize;
  }

  @Deprecated
  public Etag getLastAttachmentEtag() {
    return lastAttachmentEtag;
  }
  public Etag getLastDocEtag() {
    return lastDocEtag;
  }
  public FutureBatchStats[] getPrefetches() {
    return prefetches;
  }
  public String[] getStaleIndexes() {
    return staleIndexes;
  }
  public void setApproximateTaskCount(long approximateTaskCount) {
    this.approximateTaskCount = approximateTaskCount;
  }
  public void setCountOfDocuments(long countOfDocuments) {
    this.countOfDocuments = countOfDocuments;
  }
  public void setCountOfIndexes(int countOfIndexes) {
    this.countOfIndexes = countOfIndexes;
  }
  public void setCurrentNumberOfItemsToIndexInSingleBatch(int currentNumberOfItemsToIndexInSingleBatch) {
    this.currentNumberOfItemsToIndexInSingleBatch = currentNumberOfItemsToIndexInSingleBatch;
  }
  public void setCurrentNumberOfItemsToReduceInSingleBatch(int currentNumberOfItemsToReduceInSingleBatch) {
    this.currentNumberOfItemsToReduceInSingleBatch = currentNumberOfItemsToReduceInSingleBatch;
  }
  public void setDatabaseId(UUID databaseId) {
    this.databaseId = databaseId;
  }
  public void setDatabaseTransactionVersionSizeInMB(float databaseTransactionVersionSizeInMB) {
    this.databaseTransactionVersionSizeInMB = databaseTransactionVersionSizeInMB;
  }
  public void setErrors(IndexingError[] errors) {
    this.errors = errors;
  }
  public void setIndexes(IndexStats[] indexes) {
    this.indexes = indexes;
  }
  public void setInMemoryIndexingQueueSize(int inMemoryIndexingQueueSize) {
    this.inMemoryIndexingQueueSize = inMemoryIndexingQueueSize;
  }

  @Deprecated
  public void setLastAttachmentEtag(Etag lastAttachmentEtag) {
    this.lastAttachmentEtag = lastAttachmentEtag;
  }

  public void setLastDocEtag(Etag lastDocEtag) {
    this.lastDocEtag = lastDocEtag;
  }

  public void setPrefetches(FutureBatchStats[] prefetches) {
    this.prefetches = prefetches;
  }

  public void setStaleIndexes(String[] staleIndexes) {
    this.staleIndexes = staleIndexes;
  }


}
