package net.ravendb.abstractions.data;

import net.ravendb.abstractions.json.linq.RavenJObject;

public class PatchResultData {
  private PatchResult patchResult;
  private RavenJObject document;
  private RavenJObject debugActions;

  /**
   * Patched document.
   */
  public RavenJObject getDocument() {
    return document;
  }

  /**
   * Result of patch operation:
   * - DocumentDoesNotExists - document does not exists, operation was a no-op,
   * - Patched - document was properly patched,
   * - Tested - document was properly tested,
   * - Skipped - document was not patched, because skipPatchIfEtagMismatch was set and the etag did not match,
   * - NotModified - neither document body not metadata was changed during patch operation
   */
  public PatchResult getPatchResult() {
    return patchResult;
  }

  /**
   * Patched document.
   * @param document
   */
  public void setDocument(RavenJObject document) {
    this.document = document;
  }

  /**
   * Result of patch operation:
   * - DocumentDoesNotExists - document does not exists, operation was a no-op,
   * - Patched - document was properly patched,
   * - Tested - document was properly tested,
   * - Skipped - document was not patched, because skipPatchIfEtagMismatch was set and the etag did not match,
   * - NotModified - neither document body not metadata was changed during patch operation
   * @param patchResult
   */
  public void setPatchResult(PatchResult patchResult) {
    this.patchResult = patchResult;
  }

  /**
   * Additional debugging information (if requested).
   */
  public RavenJObject getDebugActions() {
    return debugActions;
  }

  /**
   * Additional debugging information (if requested).
   * @param debugActions
   */
  public void setDebugActions(RavenJObject debugActions) {
    this.debugActions = debugActions;
  }

}
