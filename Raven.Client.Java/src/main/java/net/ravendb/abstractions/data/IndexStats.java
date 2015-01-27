package net.ravendb.abstractions.data;

import java.util.Date;
import java.util.List;

import net.ravendb.abstractions.basic.UseSharpEnum;
import net.ravendb.abstractions.indexing.IndexLockMode;


public class IndexStats {
  private String id;
  private String name;
  private int indexingAttempts;
  private int indexingSuccesses;
  private int indexingErrors;
  private Etag lastIndexedEtag;
  private Integer indexingLag;
  private Date lastIndexedTimestamp;
  private Date lastQueryTimestamp;
  private int touchCount;
  private IndexingPriority priority;
  private Integer reduceIndexingAttempts ;
  private Integer reduceIndexingSuccesses;
  private Integer reduceIndexingErrors;
  private Etag lastReducedEtag;
  private Date lastReducedTimestamp;
  private Date createdTimestamp;
  private Date lastIndexingTime;
  private String isOnRam;
  private IndexLockMode lockMode;
  private List<String> forEntityName;
  private boolean testIndex;

  private IndexingPerformanceStats[] performance;
  public int docsCount;

  /**
   * List of all entity names (collections) for which this index is working.
   */
  public List<String> getForEntityName() {
    return forEntityName;
  }

  /**
   * List of all entity names (collections) for which this index is working.
   * @param forEntityName
   */
  public void setForEntityName(List<String> forEntityName) {
    this.forEntityName = forEntityName;
  }

  /**
   * Total number of entries in this index.
   */
  public int getDocsCount() {
    return docsCount;
  }

  /**
   * Total number of entries in this index.
   * @param docsCount
   */
  public void setDocsCount(int docsCount) {
    this.docsCount = docsCount;
  }

  /**
   * Index name.
   */
  public String getName() {
    return name;
  }

  /**
   * Shows the difference between last document etag available in database and last indexed etag.
   */
  public Integer getIndexingLag() {
    return indexingLag;
  }

  /**
   * Shows the difference between last document etag available in database and last indexed etag.
   * @param indexingLag
   */
  public void setIndexingLag(Integer indexingLag) {
    this.indexingLag = indexingLag;
  }

  /**
   * Index name.
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Index identifier.
   */
  public String getId() {
    return id;
  }

  /**
   * Index identifier.
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Indicates how many times database tried to index documents (map) using this index.
   */
  public int getIndexingAttempts() {
    return indexingAttempts;
  }

  /**
   * Indicates how many times database tried to index documents (map) using this index.
   * @param indexingAttempts
   */
  public void setIndexingAttempts(int indexingAttempts) {
    this.indexingAttempts = indexingAttempts;
  }

  /**
   * Indicates how many indexing attempts succeeded.
   */
  public int getIndexingSuccesses() {
    return indexingSuccesses;
  }

  /**
   * Indicates how many indexing attempts succeeded.
   * @param indexingSuccesses
   */
  public void setIndexingSuccesses(int indexingSuccesses) {
    this.indexingSuccesses = indexingSuccesses;
  }

  /**
   * Indicates how many indexing attempts failed.
   */
  public int getIndexingErrors() {
    return indexingErrors;
  }

  /**
   * Indicates how many indexing attempts failed.
   * @param indexingErrors
   */
  public void setIndexingErrors(int indexingErrors) {
    this.indexingErrors = indexingErrors;
  }

  /**
   * This value represents etag of last document indexed (using map) by this index.
   */
  public Etag getLastIndexedEtag() {
    return lastIndexedEtag;
  }

  /**
   * This value represents etag of last document indexed (using map) by this index.
   * @param lastIndexedEtag
   */
  public void setLastIndexedEtag(Etag lastIndexedEtag) {
    this.lastIndexedEtag = lastIndexedEtag;
  }

  /**
   * Time of last indexing for this index.
   */
  public Date getLastIndexedTimestamp() {
    return lastIndexedTimestamp;
  }

  /**
   * Time of last indexing for this index.
   * @param lastIndexedTimestamp
   */
  public void setLastIndexedTimestamp(Date lastIndexedTimestamp) {
    this.lastIndexedTimestamp = lastIndexedTimestamp;
  }

  /**
   * Time of last query for this index.
   */
  public Date getLastQueryTimestamp() {
    return lastQueryTimestamp;
  }

  /**
   * Time of last query for this index.
   * @param lastQueryTimestamp
   */
  public void setLastQueryTimestamp(Date lastQueryTimestamp) {
    this.lastQueryTimestamp = lastQueryTimestamp;
  }

  public int getTouchCount() {
    return touchCount;
  }

  public void setTouchCount(int touchCount) {
    this.touchCount = touchCount;
  }

  /**
   * Index priority (Normal, Disabled, Idle, Abandoned, Error)
   */
  public IndexingPriority getPriority() {
    return priority;
  }

  /**
   * Index priority (Normal, Disabled, Idle, Abandoned, Error)
   * @param priority
   */
  public void setPriority(IndexingPriority priority) {
    this.priority = priority;
  }

  /**
   * Indicates how many times database tried to index documents (reduce) using this index.
   */
  public Integer getReduceIndexingAttempts() {
    return reduceIndexingAttempts;
  }

  /**
   * Indicates how many times database tried to index documents (reduce) using this index.
   * @param reduceIndexingAttempts
   */
  public void setReduceIndexingAttempts(Integer reduceIndexingAttempts) {
    this.reduceIndexingAttempts = reduceIndexingAttempts;
  }

  /**
   * Indicates how many reducing attempts succeeded.
   */
  public Integer getReduceIndexingSuccesses() {
    return reduceIndexingSuccesses;
  }

  /**
   * Indicates how many reducing attempts succeeded.
   * @param reduceIndexingSuccesses
   */
  public void setReduceIndexingSuccesses(Integer reduceIndexingSuccesses) {
    this.reduceIndexingSuccesses = reduceIndexingSuccesses;
  }

  /**
   * Indicates how many reducing attempts failed.
   */
  public Integer getReduceIndexingErrors() {
    return reduceIndexingErrors;
  }

  /**
   * Indicates how many reducing attempts failed.
   * @param reduceIndexingErrors
   */
  public void setReduceIndexingErrors(Integer reduceIndexingErrors) {
    this.reduceIndexingErrors = reduceIndexingErrors;
  }

  /**
   * This value represents etag of last document indexed (using reduce) by this index.
   */
  public Etag getLastReducedEtag() {
    return lastReducedEtag;
  }

  /**
   * This value represents etag of last document indexed (using reduce) by this index.
   * @param lastReducedEtag
   */
  public void setLastReducedEtag(Etag lastReducedEtag) {
    this.lastReducedEtag = lastReducedEtag;
  }

  /**
   * Time of last reduce for this index.
   */
  public Date getLastReducedTimestamp() {
    return lastReducedTimestamp;
  }

  /**
   * Time of last reduce for this index.
   * @param lastReducedTimestamp
   */
  public void setLastReducedTimestamp(Date lastReducedTimestamp) {
    this.lastReducedTimestamp = lastReducedTimestamp;
  }

  /**
   * Date of index creation.
   */
  public Date getCreatedTimestamp() {
    return createdTimestamp;
  }

  /**
   * Date of index creation.
   * @param createdTimestamp
   */
  public void setCreatedTimestamp(Date createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  /**
   * Time of last indexing (map or reduce) for this index.
   */
  public Date getLastIndexingTime() {
    return lastIndexingTime;
  }

  /**
   * Time of last indexing (map or reduce) for this index.
   * @param lastIndexingTime
   */
  public void setLastIndexingTime(Date lastIndexingTime) {
    this.lastIndexingTime = lastIndexingTime;
  }

  /**
   * Indicates if index is in-memory only.
   */
  public String getIsOnRam() {
    return isOnRam;
  }

  /**
   * Indicates if index is in-memory only.
   * @param isOnRam
   */
  public void setIsOnRam(String isOnRam) {
    this.isOnRam = isOnRam;
  }

  /**
   * Indicates current lock mode:
   * - Unlock - all index definition changes acceptable
   * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
   * - LockedError - all index definition changes will raise exception
   */
  public IndexLockMode getLockMode() {
    return lockMode;
  }

  /**
   * Indicates current lock mode:
   * - Unlock - all index definition changes acceptable
   * - LockedIgnore - all index definition changes will be ignored, only log entry will be created
   * - LockedError - all index definition changes will raise exception
   * @param lockMode
   */
  public void setLockMode(IndexLockMode lockMode) {
    this.lockMode = lockMode;
  }

  /**
   * Performance statistics for this index.
   */
  public IndexingPerformanceStats[] getPerformance() {
    return performance;
  }

  /**
   * Performance statistics for this index.
   * @param performance
   */
  public void setPerformance(IndexingPerformanceStats[] performance) {
    this.performance = performance;
  }

  /**
   * Indicates if this is a test index (works on a limited data set - for testing purposes only)
   */
  public boolean isTestIndex() {
    return testIndex;
  }

  /**
   * Indicates if this is a test index (works on a limited data set - for testing purposes only)
   */
  public void setTestIndex(boolean testIndex) {
    this.testIndex = testIndex;
  }

  @Override
  public String toString() {
    return "IndexStats [id=" + id + "]";
  }

  @UseSharpEnum
  public static enum IndexingPriority {
    NONE(0),
    NORMAL(1),
    DISABLED(2),
    IDLE(4),
    ABANDONED(8),
    ERROR(16),
    FORCED(512);

    private int code;

    private IndexingPriority(int code) {
      this.code = code;
    }

    public int getCode() {
      return code;
    }

  }

  public static class IndexingPerformanceStats {
    private String operation;
    private int itemsCount;
    private int inputCount;
    private int outputCount;
    private Date started;
    private Date completed;
    private String duration;
    private double durationMilliseconds;
    private int loadDocumentCount;
    private long loadDocumentDurationMs;
    private String waitingTimeSinceLastBatchCompleted;

    public String getOperation() {
      return operation;
    }

    public void setOperation(String operation) {
      this.operation = operation;
    }

    public int getItemsCount() {
      return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
      this.itemsCount = itemsCount;
    }

    public int getInputCount() {
      return inputCount;
    }

    public void setInputCount(int inputCount) {
      this.inputCount = inputCount;
    }

    public int getOutputCount() {
      return outputCount;
    }

    public void setOutputCount(int outputCount) {
      this.outputCount = outputCount;
    }

    public Date getStarted() {
      return started;
    }

    public void setStarted(Date started) {
      this.started = started;
    }

    public Date getCompleted() {
      return completed;
    }

    public void setCompleted(Date completed) {
      this.completed = completed;
    }

    public String getDuration() {
      return duration;
    }

    public void setDuration(String duration) {
      this.duration = duration;
    }

    public double getDurationMilliseconds() {
      return durationMilliseconds;
    }

    public void setDurationMilliseconds(double durationMilliseconds) {
      this.durationMilliseconds = durationMilliseconds;
    }

    public int getLoadDocumentCount() {
      return loadDocumentCount;
    }

    public void setLoadDocumentCount(int loadDocumentCount) {
      this.loadDocumentCount = loadDocumentCount;
    }

    public long getLoadDocumentDurationMs() {
      return loadDocumentDurationMs;
    }

    public void setLoadDocumentDurationMs(long loadDocumentDurationMs) {
      this.loadDocumentDurationMs = loadDocumentDurationMs;
    }

    public String getWaitingTimeSinceLastBatchCompleted() {
      return waitingTimeSinceLastBatchCompleted;
    }

    public void setWaitingTimeSinceLastBatchCompleted(String waitingTimeSinceLastBatchCompleted) {
      this.waitingTimeSinceLastBatchCompleted = waitingTimeSinceLastBatchCompleted;
    }

  }

}
