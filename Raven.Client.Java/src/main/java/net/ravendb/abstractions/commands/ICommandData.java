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
   * Key of a document.
   * @return key
   */
  public String getKey();

  /**
   * Returns operation method.
   * @return method
   */
  public HttpMethods getMethod();

  /**
   * Current document etag, used for concurrency checks (null to skip check).
   * @return etag
   */
  public Etag getEtag();

  /**
   * RavenJObject representing document's metadata.
   * @return metadata
   */
  public RavenJObject getMetadata();

  /**
   * Additional command data. For internal use only.
   * @return additional data
   */
  public RavenJObject getAdditionalData();

  /**
   * Sets the additional metadata
   * @param additionalMeta
   */
  public void setAdditionalData(RavenJObject additionalMeta);

  /**
   * Translates this instance to a Json object.
   * @return RavenJObject representing the command.
   */
  public RavenJObject toJson();

}
