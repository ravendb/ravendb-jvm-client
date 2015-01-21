package net.ravendb.abstractions.data;


public class FutureBatchStats {

  private String timestamp;
  private String duration;
  private int size;
  private int retries;
  private PrefetchingUser prefetchingUser;

  /**
   * Indicates what prefetching user (indexer, replicator, sql replicator) calculated the future batch.
   */
  public PrefetchingUser getPrefetchingUser() {
    return prefetchingUser;
  }

  /**
   * Indicates what prefetching user (indexer, replicator, sql replicator) calculated the future batch.
   * @param prefetchingUser
   */
  public void setPrefetchingUser(PrefetchingUser prefetchingUser) {
    this.prefetchingUser = prefetchingUser;
  }

  /**
   * Time when future batch was created.
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * Time when future batch was created.
   * @param timestamp
   */
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Indicates how much time it took to prepare future batch.
   */
  public String getDuration() {
    return duration;
  }

  /**
   * Indicates how much time it took to prepare future batch.
   * @param duration
   */
  public void setDuration(String duration) {
    this.duration = duration;
  }

  /**
   * Number of documents in batch.
   */
  public int getSize() {
    return size;
  }

  /**
   * Number of documents in batch.
   * @param size
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * Number of retries till the future batch calculation succeeded.
   */
  public int getRetries() {
    return retries;
  }

  /**
   * Number of retries till the future batch calculation succeeded.
   * @param retries
   */
  public void setRetries(int retries) {
    this.retries = retries;
  }

}
