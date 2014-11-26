package net.ravendb.abstractions.commands;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;

/**
 * A single operation inside the batch.
 *
 */
public interface ICommandData {

  /**
   * @return key
   */
  public String getKey();

  /**
   * @return method
   */
  public HttpMethods getMethod();

  /**
   * @return etag
   */
  public Etag getEtag();

  /**
   * @return metadata
   */
  public RavenJObject getMetadata();

  /**
   * @return additional data
   */
  public RavenJObject getAdditionalData();

  /**
   * Sets the additional metadata
   * @param additionalMeta
   */
  public void setAdditionalData(RavenJObject additionalMeta);

  /**
   * Translate the instance to a Json object.
   * @return RavenJObject
   */
  public RavenJObject toJson();

}
