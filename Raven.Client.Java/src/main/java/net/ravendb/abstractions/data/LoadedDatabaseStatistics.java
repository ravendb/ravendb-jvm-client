package net.ravendb.abstractions.data;

import java.util.Date;

public class LoadedDatabaseStatistics {
  private String name;
  private Date lastActivity;

  private long transactionalStorageAllocatedSize;
  private String transactionalStorageAllocatedSizeHumaneSize;
  private long transactionalStorageUsedSize;
  private String transactionalStorageUsedSizeHumaneSize;

  private long indexStorageSize;
  private String indexStorageHumaneSize;
  private long totalDatabaseSize;
  private String totalDatabaseHumaneSize;
  private long countOfDocuments;

  @Deprecated
  private long countOfAttachments;
  private double databaseTransactionVersionSizeInMB;
  private DatabaseMetrics metrics;
  private StorageStats storageStats;

  /**
   * Database storage statistics.
   */
  public StorageStats getStorageStats() {
    return storageStats;
  }

  /**
   * Database storage statistics.
   * @param storageStats
   */
  public void setStorageStats(StorageStats storageStats) {
    this.storageStats = storageStats;
  }

  /**
   * Database metrics.
   */
  public DatabaseMetrics getMetrics() {
    return metrics;
  }

  /**
   * Database metrics.
   * @param metrics
   */
  public void setMetrics(DatabaseMetrics metrics) {
    this.metrics = metrics;
  }

  /**
   * Name of database.
   */
  public String getName() {
    return name;
  }

  /**
   * Name of database.
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Time of last activity on this database.
   */
  public Date getLastActivity() {
    return lastActivity;
  }

  /**
   * Time of last activity on this database.
   * @param lastActivity
   */
  public void setLastActivity(Date lastActivity) {
    this.lastActivity = lastActivity;
  }

  /**
   * Total count of attachments in database.
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public long getCountOfAttachments() {
    return countOfAttachments;
  }

  /**
   * Total count of attachments in database.
   * @param countOfAttachments
   * @deprecated Use RavenFS instead.
   */
  @Deprecated
  public void setCountOfAttachments(long countOfAttachments) {
    this.countOfAttachments = countOfAttachments;
  }

  /**
   * Size of a index storage in bytes.
   */
  public long getIndexStorageSize() {
    return indexStorageSize;
  }

  /**
   * Size of a index storage in bytes.
   * @param indexStorageSize
   */
  public void setIndexStorageSize(long indexStorageSize) {
    this.indexStorageSize = indexStorageSize;
  }

  /**
   * Size of a index storage in a more human readable format.
   */
  public String getIndexStorageHumaneSize() {
    return indexStorageHumaneSize;
  }

  /**
   * Size of a index storage in a more human readable format.
   * @param indexStorageHumaneSize
   */
  public void setIndexStorageHumaneSize(String indexStorageHumaneSize) {
    this.indexStorageHumaneSize = indexStorageHumaneSize;
  }

  /**
   * Total database size in bytes.
   */
  public long getTotalDatabaseSize() {
    return totalDatabaseSize;
  }

  /**
   * Total database size in bytes.
   * @param totalDatabaseSize
   */
  public void setTotalDatabaseSize(long totalDatabaseSize) {
    this.totalDatabaseSize = totalDatabaseSize;
  }

  /**
   * Total database size in a more human readable format.
   */
  public String getTotalDatabaseHumaneSize() {
    return totalDatabaseHumaneSize;
  }

  /**
   * Total database size in a more human readable format.
   * @param totalDatabaseHumaneSize
   */
  public void setTotalDatabaseHumaneSize(String totalDatabaseHumaneSize) {
    this.totalDatabaseHumaneSize = totalDatabaseHumaneSize;
  }

  /**
   * Total count of documents in database.
   */
  public long getCountOfDocuments() {
    return countOfDocuments;
  }

  /**
   * Total count of documents in database.
   * @param countOfDocuments
   */
  public void setCountOfDocuments(long countOfDocuments) {
    this.countOfDocuments = countOfDocuments;
  }

  /**
   * Transaction version size in megabytes for database.
   */
  public double getDatabaseTransactionVersionSizeInMB() {
    return databaseTransactionVersionSizeInMB;
  }

  /**
   * Transaction version size in megabytes for database.
   * @param databaseTransactionVersionSizeInMB
   */
  public void setDatabaseTransactionVersionSizeInMB(double databaseTransactionVersionSizeInMB) {
    this.databaseTransactionVersionSizeInMB = databaseTransactionVersionSizeInMB;
  }

  /**
   * Size (allocated) of a transactional storage in bytes.
   * @return
   */
  public long getTransactionalStorageAllocatedSize() {
    return transactionalStorageAllocatedSize;
  }

  /**
   * Size (allocated) of a transactional storage in bytes.
   * @param transactionalStorageAllocatedSize
   */
  public void setTransactionalStorageAllocatedSize(long transactionalStorageAllocatedSize) {
    this.transactionalStorageAllocatedSize = transactionalStorageAllocatedSize;
  }

  /**
   * Size (allocated) of a transactional storage in a more human readable format.
   */
  public String getTransactionalStorageAllocatedSizeHumaneSize() {
    return transactionalStorageAllocatedSizeHumaneSize;
  }

  /**
   * Size (allocated) of a transactional storage in a more human readable format.
   * @param transactionalStorageAllocatedSizeHumaneSize
   */
  public void setTransactionalStorageAllocatedSizeHumaneSize(String transactionalStorageAllocatedSizeHumaneSize) {
    this.transactionalStorageAllocatedSizeHumaneSize = transactionalStorageAllocatedSizeHumaneSize;
  }

  /**
   * Size (used) of a transactional storage in bytes.
   */
  public long getTransactionalStorageUsedSize() {
    return transactionalStorageUsedSize;
  }

  /**
   * Size (used) of a transactional storage in bytes.
   * @param transactionalStorageUsedSize
   */
  public void setTransactionalStorageUsedSize(long transactionalStorageUsedSize) {
    this.transactionalStorageUsedSize = transactionalStorageUsedSize;
  }

  /**
   * Size (used) of a transactional storage in a more human readable format.
   */
  public String getTransactionalStorageUsedSizeHumaneSize() {
    return transactionalStorageUsedSizeHumaneSize;
  }

  /**
   * Size (used) of a transactional storage in a more human readable format.
   * @param transactionalStorageUsedSizeHumaneSize
   */
  public void setTransactionalStorageUsedSizeHumaneSize(String transactionalStorageUsedSizeHumaneSize) {
    this.transactionalStorageUsedSizeHumaneSize = transactionalStorageUsedSizeHumaneSize;
  }

}
