package net.ravendb.abstractions.commands;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJValue;

/**
 *  A single batch operation for a document PUT
 */
public class PutCommandData implements ICommandData {
  private String key;
  private Etag etag;
  private RavenJObject document;
  private RavenJObject metadata;
  private RavenJObject additionalData;


  public PutCommandData() {
    super();
  }

  public PutCommandData(String key, Etag etag, RavenJObject document, RavenJObject metadata) {
    super();
    this.key = key;
    this.etag = etag;
    this.document = document;
    this.metadata = metadata;
  }

  /**
   * Additional command data. For internal use only.
   */
  @Override
  public RavenJObject getAdditionalData() {
    return additionalData;
  }

  /**
   * RavenJObject representing the document.
   */
  public RavenJObject getDocument() {
    return document;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check).
   */
  @Override
  public Etag getEtag() {
    return etag;
  }

  /**
   * Key of a document.
   */
  @Override
  public String getKey() {
    return key;
  }

  /**
   * RavenJObject representing document's metadata.
   */
  @Override
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * Returns operation method. In this case PUT.
   */
  @Override
  public HttpMethods getMethod() {
    return HttpMethods.PUT;
  }

  /**
   * Additional command data. For internal use only.
   */
  @Override
  public void setAdditionalData(RavenJObject additionalData) {
    this.additionalData = additionalData;
  }

  /**
   * RavenJObject representing the document.
   * @param document
   */
  public void setDocument(RavenJObject document) {
    this.document = document;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check).
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Key of a document.
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * RavenJObject representing document's metadata.
   * @param metadata
   */
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Translates this instance to a Json object.
   * @return RavenJObject representing the command.
   */
  @Override
  public RavenJObject toJson() {
    RavenJObject value = new RavenJObject();
    value.add("Key", new RavenJValue(key));
    value.add("Method", new RavenJValue(getMethod().name()));
    value.add("Document", document);
    value.add("Metadata", metadata);
    value.add("AdditionalData", additionalData);

    if (etag != null) {
      value.add("Etag", new RavenJValue(etag.toString()));
    }

    return value;
  }

}
