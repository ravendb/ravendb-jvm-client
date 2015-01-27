package net.ravendb.client.document;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.json.linq.RavenJObject;

/**
 *  Metadata held about an entity by the session
 */
public class DocumentMetadata {

  private RavenJObject originalValue;
  private RavenJObject metadata;
  private Etag etag;
  private String key;
  private RavenJObject originalMetadata;
  private boolean forceConcurrencyCheck;
  private boolean ignoreChanges;
  public RavenJObject getOriginalValue() {
    return originalValue;
  }
  public void setOriginalValue(RavenJObject originalValue) {
    this.originalValue = originalValue;
  }
  public RavenJObject getMetadata() {
    return metadata;
  }
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }
  public Etag getEtag() {
    return etag;
  }
  public void setEtag(Etag etag) {
    this.etag = etag;
  }
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }
  public RavenJObject getOriginalMetadata() {
    return originalMetadata;
  }
  public void setOriginalMetadata(RavenJObject originalMetadata) {
    this.originalMetadata = originalMetadata;
  }
  public boolean isForceConcurrencyCheck() {
    return forceConcurrencyCheck;
  }
  public void setForceConcurrencyCheck(boolean forceConcurrencyCheck) {
    this.forceConcurrencyCheck = forceConcurrencyCheck;
  }
  /**
   * If set to true, the session will ignore this document
   * when SaveChanges() is called, and won't perform and change tracking
   * @return
   */
  public boolean isIgnoreChanges() {
    return ignoreChanges;
  }
  /**
   * If set to true, the session will ignore this document
   * when SaveChanges() is called, and won't perform and change tracking
   * @return
   */
  public void setIgnoreChanges(boolean ignoreChanges) {
    this.ignoreChanges = ignoreChanges;
  }
}
