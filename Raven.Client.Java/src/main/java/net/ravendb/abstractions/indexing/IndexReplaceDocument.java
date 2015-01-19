package net.ravendb.abstractions.indexing;

import java.util.Date;

import net.ravendb.abstractions.data.Etag;

public class IndexReplaceDocument {
  private String indexToReplace;
  private Etag minimumEtagBeforeReplace;
  private Date replaceTimeUtc;

  public String getIndexToReplace() {
    return indexToReplace;
  }

  public void setIndexToReplace(String indexToReplace) {
    this.indexToReplace = indexToReplace;
  }

  public Etag getMinimumEtagBeforeReplace() {
    return minimumEtagBeforeReplace;
  }

  public void setMinimumEtagBeforeReplace(Etag minimumEtagBeforeReplace) {
    this.minimumEtagBeforeReplace = minimumEtagBeforeReplace;
  }

  public Date getReplaceTimeUtc() {
    return replaceTimeUtc;
  }

  public void setReplaceTimeUtc(Date replaceTimeUtc) {
    this.replaceTimeUtc = replaceTimeUtc;
  }

}
