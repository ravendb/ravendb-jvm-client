package net.ravendb.abstractions.data;

import java.util.Date;

import net.ravendb.abstractions.json.linq.RavenJObject;


/**
 *  Interface that is used purely internally
 */
public interface IJsonDocumentMetadata {

  /**
   * RavenJObject representing document's metadata.
   */
  public RavenJObject getMetadata();

  /**
   * RavenJObject representing document's metadata.
   * @param obj
   */
  public void setMetadata(RavenJObject obj);

  /**
   *Key of a document.
   */
  public String getKey();

  /**
   * Key of a document.
   * @param key
   */
  public void setKey(String key);

  /**
   * Indicates whether this document is non authoritative (modified by uncommitted transaction).
   * @return the nonAuthoritativeInformation
   */
  public Boolean getNonAuthoritativeInformation();

  /**
   * Indicates whether this document is non authoritative (modified by uncommitted transaction).
   * @param nonAuthoritativeInformation
   */
  public void setNonAuthoritativeInformation(Boolean nonAuthoritativeInformation);

  /**
   * Current document etag, used for concurrency checks (null to skip check)
   */
  public Etag getEtag();

  /**
   * Current document etag, used for concurrency checks (null to skip check)
   * @param etag
   */
  public void setEtag(Etag etag);

  /**
   * Last modified date for the document
   */
  public Date getLastModified();

  /**
   * Last modified date for the document
   * @param lastModified
   */
  public void setLastModified(Date lastModified);


}
