package net.ravendb.abstractions.data;

import java.util.Date;

import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJValue;
import net.ravendb.abstractions.util.NetDateFormat;


/**
 * A document representation:
 * - Data / Projection
 * - Etag
 * - Metadata
 */
public class JsonDocument implements IJsonDocumentMetadata {
  private RavenJObject dataAsJson;
  private RavenJObject metadata;
  private String key;
  private Boolean nonAuthoritativeInformation;
  private Etag etag;
  private Date lastModified;
  private Float tempIndexScore;

  public JsonDocument(RavenJObject dataAsJson, RavenJObject metadata, String key, Boolean nonAuthoritativeInformation, Etag etag, Date lastModified) {
    super();
    this.dataAsJson = dataAsJson;
    this.metadata = metadata;
    this.key = key;
    this.nonAuthoritativeInformation = nonAuthoritativeInformation;
    this.etag = etag;
    this.lastModified = lastModified;
  }

  /**
   * Document data or projection as json.
   */
  public RavenJObject getDataAsJson() {
    return dataAsJson != null ? dataAsJson : new RavenJObject();
  }

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
    if (metadata == null) {
      metadata = new RavenJObject(String.CASE_INSENSITIVE_ORDER);
    }
    return metadata;
  }

  /**
   * The ranking of this result in the current query
   */
  public Float getTempIndexScore() {
    return tempIndexScore;
  }

  /**
   * Indicates whether this document is non authoritative (modified by uncommitted transaction).
   */
  @Override
  public Boolean getNonAuthoritativeInformation() {
    return nonAuthoritativeInformation;
  }

  /**
   * Document data or projection as json.
   * @param dataAsJson
   */
  public void setDataAsJson(RavenJObject dataAsJson) {
    this.dataAsJson = dataAsJson;
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

  /**
   * The ranking of this result in the current query
   * @param tempIndexScore
   */
  public void setTempIndexScore(Float tempIndexScore) {
    this.tempIndexScore = tempIndexScore;
  }

  /**
   * Translate the json document to a {@link RavenJObject}
   */
  public RavenJObject toJson() {
    return toJson(false);
  }

  /**
   * Translate the json document to a {@link RavenJObject}
   */
  @SuppressWarnings({"hiding", "boxing"})
  public RavenJObject toJson(boolean checkForId) {
    dataAsJson.ensureCannotBeChangeAndEnableShapshotting();
    metadata.ensureCannotBeChangeAndEnableShapshotting();

    RavenJObject doc = dataAsJson.createSnapshot();
    RavenJObject metadata = this.metadata.createSnapshot();

    if (lastModified != null) {
      NetDateFormat fdf = new NetDateFormat();
      metadata.add(Constants.LAST_MODIFIED, new RavenJValue(this.lastModified));
      metadata.add(Constants.RAVEN_LAST_MODIFIED, new RavenJValue(fdf.format(this.lastModified)));
    }
    if (etag != null) {
      metadata.add("@etag", new RavenJValue(etag.toString()));
    }
    if (nonAuthoritativeInformation) {
      metadata.add("Non-Authoritative-Information", new RavenJValue(nonAuthoritativeInformation));
    }

    if (checkForId && !metadata.containsKey("@id")) {
      metadata.add("@id", key);
    }

    doc.add("@metadata", metadata);

    return doc;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "JsonDocument [dataAsJson=" + dataAsJson + ", metadata=" + metadata + ", key=" + key + ", etag=" + etag + ", lastModified=" + lastModified + "]";
  }

}
