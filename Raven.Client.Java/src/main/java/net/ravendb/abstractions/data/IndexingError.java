package net.ravendb.abstractions.data;

import java.util.Date;

public class IndexingError {
  private long id;
  private int index;
  private String indexName;
  private String error;
  private Date timestamp;
  private String document;
  private String action;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public int getIndex() {
    return index;
  }


  public void setIndex(int index) {
    this.index = index;
  }


  public String getIndexName() {
    return indexName;
  }


  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getDocument() {
    return document;
  }

  public void setDocument(String document) {
    this.document = document;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public String toString() {
    return "IndexingError [id = " + id + ", index=" + index + ", error=" + error + ", timestamp=" + timestamp + ", document=" + document + ", action=" + action + "]";
  }


}
