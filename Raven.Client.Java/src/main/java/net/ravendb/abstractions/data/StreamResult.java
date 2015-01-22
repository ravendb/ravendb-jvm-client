package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;

public class StreamResult<T> {
  private String key;
  private Etag etag;
  private RavenJObject metadata;
  private T document;

  /**
   * Document key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Document key.
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Document etag.
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Document etag.
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Document metadata.
   */
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * Document metadata.
   * @param metadata
   */
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Document deserialized to T.
   */
  public T getDocument() {
    return document;
  }

  /**
   * Document deserialized to T.
   * @param document
   */
  public void setDocument(T document) {
    this.document = document;
  }


}
