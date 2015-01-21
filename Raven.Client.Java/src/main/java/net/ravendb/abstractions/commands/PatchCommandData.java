package net.ravendb.abstractions.commands;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.PatchRequest;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJValue;

public class PatchCommandData implements ICommandData {

  private PatchRequest[] patches;
  private PatchRequest[] patchesIfMissing;
  private String key;
  private Etag etag;
  private RavenJObject metadata;
  private RavenJObject additionalData;
  private boolean skipPatchIfEtagMismatch;

  /**
   * If set to true, _and_ the Etag is specified then the behavior
   * of the patch in the case of etag mismatch is different. Instead of throwing,
   * the patch operation wouldn't complete, and the Skipped status would be returned
   * to the user for this operation
   */
  public boolean isSkipPatchIfEtagMismatch() {
    return skipPatchIfEtagMismatch;
  }

  /**
   * If set to true, _and_ the Etag is specified then the behavior
   * of the patch in the case of etag mismatch is different. Instead of throwing,
   * the patch operation wouldn't complete, and the Skipped status would be returned
   * to the user for this operation
   * @param skipPatchIfEtagMismatch
   */
  public void setSkipPatchIfEtagMismatch(boolean skipPatchIfEtagMismatch) {
    this.skipPatchIfEtagMismatch = skipPatchIfEtagMismatch;
  }

  /**
   * Additional command data. For internal use only.
   */
  @Override
  public RavenJObject getAdditionalData() {
    return additionalData;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check)
   */
  @Override
  public Etag getEtag() {
    return etag;
  }

  /**
   * Key of a document to patch.
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
   * Returns operation method. In this case PATCH.
   */
  @Override
  public HttpMethods getMethod() {
    return HttpMethods.PATCH;
  }

  /**
   * Array of patches that will be applied to the document
   */
  public PatchRequest[] getPatches() {
    return patches;
  }

  /**
   * Array of patches to apply to a default document if the document is missing
   */
  public PatchRequest[] getPatchesIfMissing() {
    return patchesIfMissing;
  }

  /**
   * Additional command data. For internal use only.
   * @param additionalData
   */
  @Override
  public void setAdditionalData(RavenJObject additionalData) {
    this.additionalData = additionalData;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check)
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Key of a document to patch.
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
   * Array of patches that will be applied to the document
   * @param patches
   */
  public void setPatches(PatchRequest[] patches) {
    this.patches = patches;
  }

  /**
   * Array of patches to apply to a default document if the document is missing
   * @param patchesIfMissing
   */
  public void setPatchesIfMissing(PatchRequest[] patchesIfMissing) {
    this.patchesIfMissing = patchesIfMissing;
  }

  /**
   * Translates this instance to a Json object.
   * @return RavenJObject representing the command.
   */
  @Override
  public RavenJObject toJson() {
    RavenJObject ret = new RavenJObject();
    ret.add("Key", new RavenJValue(key));
    ret.add("Method", new RavenJValue(getMethod().name()));

    RavenJArray patchesArray = new RavenJArray();
    for (PatchRequest patchRequest: patches) {
      patchesArray.add(patchRequest.toJson());
    }
    ret.add("Patches", patchesArray);
    ret.add("Metadata", metadata);
    ret.add("AdditionalData", additionalData);
    ret.add("SkipPatchIfEtagMismatch", skipPatchIfEtagMismatch);
    if (etag != null) {
      ret.add("Etag", new RavenJValue(etag));
    }
    if (patchesIfMissing != null && patchesIfMissing.length > 0) {
      RavenJArray patchesIfMissingArray = new RavenJArray();
      for (PatchRequest patchRequest: patchesIfMissing) {
        patchesIfMissingArray.add(patchRequest.toJson());
      }
      ret.add("PatchesIfMissing", patchesIfMissingArray);
    }
    return ret;
  }

}
