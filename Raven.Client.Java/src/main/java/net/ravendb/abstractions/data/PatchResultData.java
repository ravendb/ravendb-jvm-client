package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;

public class PatchResultData {
  private PatchResult patchResult;
  private RavenJObject document;
  private RavenJObject debugActions;

  public RavenJObject getDocument() {
    return document;
  }
  public PatchResult getPatchResult() {
    return patchResult;
  }
  public void setDocument(RavenJObject document) {
    this.document = document;
  }
  public void setPatchResult(PatchResult patchResult) {
    this.patchResult = patchResult;
  }

  public RavenJObject getDebugActions() {
    return debugActions;
  }

  public void setDebugActions(RavenJObject debugActions) {
    this.debugActions = debugActions;
  }

}
