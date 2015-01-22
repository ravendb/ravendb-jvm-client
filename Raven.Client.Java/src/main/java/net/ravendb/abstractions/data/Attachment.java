package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;

/**
 * @deprecated Use RavenFS instead.
 */
@Deprecated
public class Attachment {
  private byte[] data;
  private int size;
  private RavenJObject metadata;
  private Etag etag;
  private String key;
  private boolean canGetData;

  /**
   * Returning the content of an attachment.
   * @return data
   */
  public byte[] getData() {
    if (!canGetData) {
      throw new IllegalArgumentException("Cannot get attachment data because it was NOT loaded using GET method");
    }
    return data;
  }

  /**
   * Setting the content of an attachment.
   * @param data
   */
  public void setData(byte[] data) {
    this.data = data;
  }

  /**
   * Attachment size in bytes.
   * <p>
   * The max size of an attachment can be 2GB.
   * </p>
   * @return size
   */
  public int getSize() {
    return size;
  }

  /**
   * Attachment size in bytes.
   * <p>
   * The max size of an attachment can be 2GB.
   * </p>
   * @param size
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * RavenJObject representing attachment's metadata.
   * @return metadata
   */
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * RavenJObject representing attachment's metadata.
   * @param metadata
   */
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Current attachment etag, used for concurrency checks (null to skip check).
   * @return etag
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Current attachment etag, used for concurrency checks (null to skip check).
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Key of an attachment.
   */
  public String getKey() {
    return key;
  }

  /**
   * Key of an attachment.
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  public Attachment(boolean canGetData, byte[] data, int size, RavenJObject metadata, Etag etag, String key) {
    super();
    this.canGetData = canGetData;
    this.data = data;
    this.size = size;
    this.metadata = metadata;
    this.etag = etag;
    this.key = key;
  }

}
