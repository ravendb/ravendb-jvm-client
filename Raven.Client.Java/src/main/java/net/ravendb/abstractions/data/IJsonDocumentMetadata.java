package net.ravendb.abstractions.data;

import java.util.Date;

import net.ravendb.abstractions.json.linq.RavenJObject;


/**
 *  Interface that is used purely internally
 */
public interface IJsonDocumentMetadata {
  /**
   * @return metadata for the document
   */
  public RavenJObject getMetadata();

  /**
   * Sets the metadata for the document
   * @param obj
   */
  public void setMetadata(RavenJObject obj);

  /**
   * @return the key
   */
  public String getKey();

  /**
   * @param key the key to set
   */
  public void setKey(String key);

  /**
   * Gets a value indicating whether this document is non authoritative (modified by uncommitted transaction).
   * @return the nonAuthoritativeInformation
   */
  public Boolean getNonAuthoritativeInformation();

  /**
   * Sets a value indicating whether this document is non authoritative (modified by uncommitted transaction).
   * @param nonAuthoritativeInformation
   */
  public void setNonAuthoritativeInformation(Boolean nonAuthoritativeInformation);

  /**
   * @return the etag
   */
  public Etag getEtag();

  /**
   * @param etag the etag to set
   */
  public void setEtag(Etag etag);

  /**
   * @return the lastModified
   */
  public Date getLastModified();

  /**
   * @param lastModified the lastModified to set
   */
  public void setLastModified(Date lastModified);


}
