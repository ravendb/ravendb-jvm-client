package net.ravendb.abstractions.replication;

import net.ravendb.abstractions.data.Etag;


public class ReplicatedEtagInfo {

  private String destionationUrl;
  private Etag documentEtag;
  private Etag attachmentEtag;

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

  public Etag getAttachmentEtag() {
    return attachmentEtag;
  }

  public void setAttachmentEtag(Etag attachmentEtag) {
    this.attachmentEtag = attachmentEtag;
  }

}
