package net.ravendb.abstractions.data;

import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.data.IndexStats.IndexingPerformanceStats;

public class IndexingBatchInfo {

  private BatchType batchType;

  private List<String> indexesToWorkOn;

  private int totalDocumentCount;
  private long totalDocumentSize;
  private String startedAt;

  private String totalDuration;

  private Map<String, IndexingPerformanceStats> performanceStats;

  public int getTotalDocumentCount() {
    return totalDocumentCount;
  }

  public void setTotalDocumentCount(int totalDocumentCount) {
    this.totalDocumentCount = totalDocumentCount;
  }

  public long getTotalDocumentSize() {
    return totalDocumentSize;
  }

  public void setTotalDocumentSize(long totalDocumentSize) {
    this.totalDocumentSize = totalDocumentSize;
  }

  public BatchType getBatchType() {
    return batchType;
  }

  public void setBatchType(BatchType batchType) {
    this.batchType = batchType;
  }

  public List<String> getIndexesToWorkOn() {
    return indexesToWorkOn;
  }

  public void setIndexesToWorkOn(List<String> indexesToWorkOn) {
    this.indexesToWorkOn = indexesToWorkOn;
  }

  public String getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(String startedAt) {
    this.startedAt = startedAt;
  }

  public String getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(String totalDuration) {
    this.totalDuration = totalDuration;
  }

  public Map<String, IndexingPerformanceStats> getPerformanceStats() {
    return performanceStats;
  }

  public void setPerformanceStats(Map<String, IndexingPerformanceStats> performanceStats) {
    this.performanceStats = performanceStats;
  }

}
