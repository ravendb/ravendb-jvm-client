package net.ravendb.abstractions.data;

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

  private String storageEngine;

  private Etag lastDocEtag;

  @Deprecated
  private Etag lastAttachmentEtag;
  private int countOfIndexes;
  private int countOfResultTransformers;
  private int[] inMemoryIndexingQueueSize;
  private long approximateTaskCount;
  private long countOfDocuments;

  @Deprecated
  private long countOfAttachments;
  private String[] staleIndexes;
  private int currentNumberOfParallelTasks;
  private int currentNumberOfItemsToIndexInSingleBatch;
  private int currentNumberOfItemsToReduceInSingleBatch;
  private float databaseTransactionVersionSizeInMB;
  private IndexStats[] indexes;
  private IndexingError[] errors;
  private FutureBatchStats[] prefetches;
  private UUID databaseId;
  private boolean supportsDtc;

  /**
   * Total number of transformers in database.
   */
  public int getCountOfResultTransformers() {
    return countOfResultTransformers;
  }

  /**
   * Total number of transformers in database.
   * @param countOfResultTransformers
   */
  public void setCountOfResultTransformers(int countOfResultTransformers) {
    this.countOfResultTransformers = countOfResultTransformers;
  }

  /**
   * Storage engine used by database (esent, voron).
   */
  public String getStorageEngine() {
    return storageEngine;
  }

  /**
   * Storage engine used by database (esent, voron).
   * @param storageEngine
   */
  public void setStorageEngine(String storageEngine) {
    this.storageEngine = storageEngine;
  }

  /**
   * Indicates if database supports DTC transactions.
   */
  public boolean isSupportsDtc() {
    return supportsDtc;
  }

  /**
   * Indicates if database supports DTC transactions.
   * @param supportsDtc
   */
  public void setSupportsDtc(boolean supportsDtc) {
    this.supportsDtc = supportsDtc;
  }

  /**
   * Indicates how many tasks (approximately) are running currently in database.
   */
  public long getApproximateTaskCount() {
    return approximateTaskCount;
  }

  /**
   * Total number of attachments in database.
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public long getCountOfAttachments() {
    return countOfAttachments;
  }

  /**
   * Total number of attachments in database.
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void setCountOfAttachments(long countOfAttachments) {
    this.countOfAttachments = countOfAttachments;
  }

  /**
   * Total number of documents in database.
   */
  public long getCountOfDocuments() {
    return countOfDocuments;
  }

  /**
   * Total number of indexes in database.
   */
  public int getCountOfIndexes() {
    return countOfIndexes;
  }

  /**
   * The concurrency level that RavenDB is currently using
   */
  public int getCurrentNumberOfParallelTasks() {
    return currentNumberOfParallelTasks;
  }

  /**
   * The concurrency level that RavenDB is currently using
   */
  public void setCurrentNumberOfParallelTasks(int currentNumberOfParallelTasks) {
    this.currentNumberOfParallelTasks = currentNumberOfParallelTasks;
  }

  /**
   * Current value of items that will be processed by index (map) in single batch.
   */
  public int getCurrentNumberOfItemsToIndexInSingleBatch() {
    return currentNumberOfItemsToIndexInSingleBatch;
  }

  /**
   * Current value of items that will be processed by index (reduce) in single batch.
   */
  public int getCurrentNumberOfItemsToReduceInSingleBatch() {
    return currentNumberOfItemsToReduceInSingleBatch;
  }

  /**
   * Database identifier.
   * @return
   */
  public UUID getDatabaseId() {
    return databaseId;
  }

  /**
   * Transaction version size in megabytes for database.
   */
  public float getDatabaseTransactionVersionSizeInMB() {
    return databaseTransactionVersionSizeInMB;
  }

  /**
   * Array of indexing errors that occured in database.
   */
  public IndexingError[] getErrors() {
    return errors;
  }

  /**
   * Statistics for each index in database.
   */
  public IndexStats[] getIndexes() {
    return indexes;
  }

  /**
   * Indicates how many elements are currently kept in queue for all indexing prefetchers available.
   */
  public int[] getInMemoryIndexingQueueSize() {
    return inMemoryIndexingQueueSize;
  }

  /**
   * Last attachment etag in database.
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public Etag getLastAttachmentEtag() {
    return lastAttachmentEtag;
  }

  /**
   * Last document etag in database.
   */
  public Etag getLastDocEtag() {
    return lastDocEtag;
  }

  /**
   * Information about future indexing batches.
   */
  public FutureBatchStats[] getPrefetches() {
    return prefetches;
  }

  /**
   * List of stale index names in database.
   */
  public String[] getStaleIndexes() {
    return staleIndexes;
  }

  /**
   * Indicates how many tasks (approximately) are running currently in database.
   * @param approximateTaskCount
   */
  public void setApproximateTaskCount(long approximateTaskCount) {
    this.approximateTaskCount = approximateTaskCount;
  }

  /**
   * Total number of documents in database.
   * @param countOfDocuments
   */
  public void setCountOfDocuments(long countOfDocuments) {
    this.countOfDocuments = countOfDocuments;
  }

  /**
   * Total number of indexes in database.
   * @param countOfIndexes
   */
  public void setCountOfIndexes(int countOfIndexes) {
    this.countOfIndexes = countOfIndexes;
  }

  /**
   * Current value of items that will be processed by index (map) in single batch.
   * @param currentNumberOfItemsToIndexInSingleBatch
   */
  public void setCurrentNumberOfItemsToIndexInSingleBatch(int currentNumberOfItemsToIndexInSingleBatch) {
    this.currentNumberOfItemsToIndexInSingleBatch = currentNumberOfItemsToIndexInSingleBatch;
  }

  /**
   * Current value of items that will be processed by index (reduce) in single batch.
   * @param currentNumberOfItemsToReduceInSingleBatch
   */
  public void setCurrentNumberOfItemsToReduceInSingleBatch(int currentNumberOfItemsToReduceInSingleBatch) {
    this.currentNumberOfItemsToReduceInSingleBatch = currentNumberOfItemsToReduceInSingleBatch;
  }

  /**
   * Database identifier.
   * @param databaseId
   */
  public void setDatabaseId(UUID databaseId) {
    this.databaseId = databaseId;
  }

  /**
   * Transaction version size in megabytes for database.
   * @param databaseTransactionVersionSizeInMB
   */
  public void setDatabaseTransactionVersionSizeInMB(float databaseTransactionVersionSizeInMB) {
    this.databaseTransactionVersionSizeInMB = databaseTransactionVersionSizeInMB;
  }

  /**
   * Array of indexing errors that occured in database.
   * @param errors
   */
  public void setErrors(IndexingError[] errors) {
    this.errors = errors;
  }

  /**
   * Statistics for each index in database.
   * @param indexes
   */
  public void setIndexes(IndexStats[] indexes) {
    this.indexes = indexes;
  }

  /**
   * Indicates how many elements are currently kept in queue for all indexing prefetchers available.
   * @param inMemoryIndexingQueueSize
   */
  public void setInMemoryIndexingQueueSize(int[] inMemoryIndexingQueueSize) {
    this.inMemoryIndexingQueueSize = inMemoryIndexingQueueSize;
  }

  /**
   * Last attachment etag in database.
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void setLastAttachmentEtag(Etag lastAttachmentEtag) {
    this.lastAttachmentEtag = lastAttachmentEtag;
  }

  /**
   * Last document etag in database.
   * @param lastDocEtag
   */
  public void setLastDocEtag(Etag lastDocEtag) {
    this.lastDocEtag = lastDocEtag;
  }

  /**
   * Information about future indexing batches.
   * @param prefetches
   */
  public void setPrefetches(FutureBatchStats[] prefetches) {
    this.prefetches = prefetches;
  }

  /**
   * List of stale index names in database.
   * @param staleIndexes
   */
  public void setStaleIndexes(String[] staleIndexes) {
    this.staleIndexes = staleIndexes;
  }


}
