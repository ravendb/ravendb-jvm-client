package net.ravendb.abstractions.replication;

import net.ravendb.abstractions.data.Etag;


public class ReplicatedEtagInfo {

  private String destionationUrl;
  private Etag documentEtag;

  public String getDestionationUrl() {
    return destionationUrl;
  }

  public void setDestionationUrl(String destionationUrl) {
    this.destionationUrl = destionationUrl;
  }

  public Etag getDocumentEtag() {
    return documentEtag;
  }

  public void setDocumentEtag(Etag documentEtag) {
    this.documentEtag = documentEtag;
  }

  @Override
  public String toString() {
    return String.format("Url: %s, Etag: %s", destionationUrl, documentEtag == null ? "no etag" : documentEtag.toString());
  }
}
