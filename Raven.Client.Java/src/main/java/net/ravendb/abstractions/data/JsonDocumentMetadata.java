package net.ravendb.abstractions.data;

import java.util.Date;

import net.ravendb.abstractions.json.linq.RavenJObject;


public class JsonDocumentMetadata implements IJsonDocumentMetadata {

  private RavenJObject metadata;
  private String key;
  private Boolean nonAuthoritativeInformation;
  private Etag etag;
  private Date lastModified;

  /**
   * Current document etag.
   */
  @Override
  public Etag getEtag() {
    return etag;
  }

  /**
   * Key for the document
   */
  @Override
  public String getKey() {
    return key;
  }

  /**
   * Last modified date for the document
   */
  @Override
  public Date getLastModified() {
    return lastModified;
  }

  /**
   * Metadata for the document
   */
  @Override
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * Indicates whether this document is non authoritative (modified by uncommitted transaction).
   */
  @Override
  public Boolean getNonAuthoritativeInformation() {
    return nonAuthoritativeInformation;
  }

  /**
   * Current document etag.
   * @param etag
   */
  @Override
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Key for the document
   * @param key
   */
  @Override
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Last modified date for the document
   * @param lastModified
   */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Metadata for the document
   * @param metadata
   */
  @Override
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Indicates whether this document is non authoritative (modified by uncommitted transaction).
   * @param nonAuthoritativeInformation
   */
  @Override
  public void setNonAuthoritativeInformation(Boolean nonAuthoritativeInformation) {
    this.nonAuthoritativeInformation = nonAuthoritativeInformation;
  }

}
