package net.ravendb.abstractions.data;


public class IndexingBatchInfo {
  private int totalDocumentCount;
  private long totalDocumentSize;
  private String timestamp;

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

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + totalDocumentCount;
    result = prime * result + (int) (totalDocumentSize ^ (totalDocumentSize >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    IndexingBatchInfo other = (IndexingBatchInfo) obj;
    if (totalDocumentCount != other.totalDocumentCount) return false;
    if (totalDocumentSize != other.totalDocumentSize) return false;
    return true;
  }

}
