package net.ravendb.abstractions.commands;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.data.ScriptedPatchRequest;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJValue;

/**
 * A single batch operation for a document EVAL (using a Javascript)
 */
public class ScriptedPatchCommandData implements ICommandData {
  private ScriptedPatchRequest patch;
  private ScriptedPatchRequest patchIfMissing;
  private String key;
  private Etag etag;
  private RavenJObject metadata;
  private boolean debugMode;
  private RavenJObject additionalData;

  /**
   * Returns operation method. In this case EVAL.
   */
  @Override
  public HttpMethods getMethod() {
    return HttpMethods.EVAL;
  }

  /**
   * ScriptedPatchRequest (using JavaScript) that is used to patch the document.
   */
  public ScriptedPatchRequest getPatch() {
    return patch;
  }

  /**
   * ScriptedPatchRequest (using JavaScript) that is used to patch the document.
   * @param patch
   */
  public void setPatch(ScriptedPatchRequest patch) {
    this.patch = patch;
  }

  /**
   * ScriptedPatchRequest (using JavaScript) that is used to patch a default document if the document is missing.
   */
  public ScriptedPatchRequest getPatchIfMissing() {
    return patchIfMissing;
  }

  /**
   * ScriptedPatchRequest (using JavaScript) that is used to patch a default document if the document is missing.
   * @param patchIfMissing
   */
  public void setPatchIfMissing(ScriptedPatchRequest patchIfMissing) {
    this.patchIfMissing = patchIfMissing;
  }

  /**
   * Key of a document to patch.
   */
  @Override
  public String getKey() {
    return key;
  }

  /**
   * Key of a document to patch.
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check).
   */
  @Override
  public Etag getEtag() {
    return etag;
  }

  /**
   * Current document etag, used for concurrency checks (null to skip check).
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * RavenJObject representing document's metadata.
   */
  @Override
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * RavenJObject representing document's metadata.
   * @param metadata
   */
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * Indicates in the operation should be run in debug mode. If set to true, then server will return additional information in response.
   */
  public boolean isDebugMode() {
    return debugMode;
  }

  /**
   * Indicates in the operation should be run in debug mode. If set to true, then server will return additional information in response.
   * @param debugMode
   */
  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  /**
   * Additional command data. For internal use only.
   */
  @Override
  public RavenJObject getAdditionalData() {
    return additionalData;
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
   * Translates this instance to a Json object.
   * @return RavenJObject representing the command.
   */
  @Override
  public RavenJObject toJson() {
    RavenJObject ret = new RavenJObject();
    ret.add("Key", new RavenJValue(key));
    ret.add("Method", RavenJValue.fromObject(getMethod().name()));

    RavenJObject patch = new RavenJObject();
    patch.add("Script", new RavenJValue(this.patch.getScript()));
    patch.add("Values", RavenJObject.fromObject(this.patch.getValues()));

    ret.add("Patch", patch);
    ret.add("DebugMode", new RavenJValue(debugMode));
    ret.add("AdditionalData", additionalData);
    ret.add("Metadata", metadata);

    if (etag != null) {
      ret.add("Etag", new RavenJValue(etag.toString()));
    }
    if (patchIfMissing != null) {
      RavenJObject patchIfMissing = new RavenJObject();
      patchIfMissing.add("Script", new RavenJValue(this.patchIfMissing.getScript()));
      patchIfMissing.add("Values", RavenJObject.fromObject(this.patchIfMissing.getValues()));
      ret.add("PatchIfMissing", patchIfMissing);
    }
    return ret;
  }

}