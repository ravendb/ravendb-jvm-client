package net.ravendb.abstractions.data;

/**
 * Options used during BulkInsert execution.
 */
public class BulkInsertOptions {
  private boolean overwriteExisting;
  private boolean checkReferencesInIndexes;
  private int batchSize;
  private int writeTimeoutMiliseconds;
  private boolean skipOverwriteIfUnchanged;
  private ChunkedBulkInsertOptions chunkedBulkInsertOptions;

  public BulkInsertOptions() {
    batchSize = 512;
    writeTimeoutMiliseconds = 15 * 1000;
    chunkedBulkInsertOptions = new ChunkedBulkInsertOptions();
    chunkedBulkInsertOptions.setMaxDocumentsPerChunk(batchSize * 4);
    chunkedBulkInsertOptions.setMaxChunkVolumeInBytes(8 * 1024 * 1024);
  }

  /**
   * Represents options of the chunked functionality of the bulk insert operation,
   * which allows opening new connection for each chunk by amount of documents and total size.
   * If Set to null, bulk insert will be performed in a not chunked manner.
   */
  public ChunkedBulkInsertOptions getChunkedBulkInsertOptions() {
    return chunkedBulkInsertOptions;
  }

  /**
   * Represents options of the chunked functionality of the bulk insert operation,
   * which allows opening new connection for each chunk by amount of documents and total size.
   * If Set to null, bulk insert will be performed in a not chunked manner.
   */
  public void setChunkedBulkInsertOptions(ChunkedBulkInsertOptions chunkedBulkInsertOptions) {
    this.chunkedBulkInsertOptions = chunkedBulkInsertOptions;
  }

  /**
   * Determines whether should skip to overwrite a document when it is updated by exactly the same document (by comparing a content and metadata as well).
   */
  public boolean isSkipOverwriteIfUnchanged() {
    return skipOverwriteIfUnchanged;
  }

  /**
   * Determines whether should skip to overwrite a document when it is updated by exactly the same document (by comparing a content and metadata as well).
   * @param skipOverwriteIfUnchanged
   */
  public void setSkipOverwriteIfUnchanged(boolean skipOverwriteIfUnchanged) {
    this.skipOverwriteIfUnchanged = skipOverwriteIfUnchanged;
  }

  /**
   * Indicates in existing documents should be overwritten. If not, exception will be thrown.
   */
  public boolean isOverwriteExisting() {
    return overwriteExisting;
  }

  /**
   * Indicates in existing documents should be overwritten. If not, exception will be thrown.
   * @param overwriteExisting
   */
  public void setOverwriteExisting(boolean overwriteExisting) {
    this.overwriteExisting = overwriteExisting;
  }

  /**
   * Indicates if referenced documents should be checked in indexes.
   */
  public boolean isCheckReferencesInIndexes() {
    return checkReferencesInIndexes;
  }

  /**
   * Indicates if referenced documents should be checked in indexes.
   * @param checkReferencesInIndexes
   */
  public void setCheckReferencesInIndexes(boolean checkReferencesInIndexes) {
    this.checkReferencesInIndexes = checkReferencesInIndexes;
  }

  /**
   * Number of documents to send in each bulk insert batch.
   * Value:
   * 512 by default
   * {@value 512 by default}
   */
  public int getBatchSize() {
    return batchSize;
  }

  /**
   * Number of documents to send in each bulk insert batch.
   * Value:
   * 512 by default
   * {@value 512 by default}
   * @param batchSize
   */
  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  /**
   * Maximum timeout in milliseconds to wait for document write. Exception will be thrown when timeout is elapsed.
   * Value:
   * 15000 milliseconds by default
   * {@value 15000 milliseconds by default}
   */
  public int getWriteTimeoutMiliseconds() {
    return writeTimeoutMiliseconds;
  }

  /**
   * Maximum timeout in milliseconds to wait for document write. Exception will be thrown when timeout is elapsed.
   * Value:
   * 15000 milliseconds by default
   * {@value 15000 milliseconds by default}
   * @param writeTimeoutMiliseconds
   */
  public void setWriteTimeoutMiliseconds(int writeTimeoutMiliseconds) {
    this.writeTimeoutMiliseconds = writeTimeoutMiliseconds;
  }

}
