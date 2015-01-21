package net.ravendb.abstractions.data;

public class PutResult {
  private String key;
  private Etag etag;

  public PutResult(String key, Etag etag) {
    super();
    this.key = key;
    this.etag = etag;
  }

  /**
   * Etag of the document after PUT operation.
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Key of the document that was PUT.
   */
  public String getKey() {
    return key;
  }


  /**
   * Etag of the document after PUT operation.
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Key of the document that was PUT.
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "PutResult [key=" + key + ", etag=" + etag + "]";
  }

}
