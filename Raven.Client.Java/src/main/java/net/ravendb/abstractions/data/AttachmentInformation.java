package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;

/**
 * Describes an attachment, but without the actual attachment data
 * @deprecated Use RavenFS instead.
 */
@Deprecated
public class AttachmentInformation {
  private int size;
  private String key;
  private RavenJObject metadata;
  private Etag etag;

  /**
   * Attachment size in bytes.
   * Remarks:
   * - max size of an attachment can be 2GB
   */
  public int getSize() {
    return size;
  }

  /**
   * Attachment size in bytes.
   * Remarks:
   * - max size of an attachment can be 2GB
   * @param size
   */
  public void setSize(int size) {
    this.size = size;
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

  /**
   * RavenJObject representing attachment's metadata.
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
   * Current attachment etag.
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Current attachment etag.
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

}
