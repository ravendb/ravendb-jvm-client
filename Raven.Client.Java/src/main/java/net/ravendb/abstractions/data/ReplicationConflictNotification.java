package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.EventArgs;


public class ReplicationConflictNotification extends EventArgs {
  private ReplicationConflictTypes itemType;
  private String id;
  private Etag etag;
  private ReplicationOperationTypes operationType;
  private String[] conflicts;

  @Override
  public String toString() {
    return String.format("%s on %s because of %s operation", itemType, id, operationType);
  }

  /**
   * Type of conflict that occured (None, DocumentReplicationConflict, AttachmentReplicationConflict).
   */
  public ReplicationConflictTypes getItemType() {
    return itemType;
  }

  /**
   * Type of conflict that occured (None, DocumentReplicationConflict, AttachmentReplicationConflict).
   * @param itemType
   */
  public void setItemType(ReplicationConflictTypes itemType) {
    this.itemType = itemType;
  }

  /**
   * Identifier of a document/attachment on which replication conflict occured.
   */
  public String getId() {
    return id;
  }

  /**
   * Identifier of a document/attachment on which replication conflict occured.
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Current conflict document Etag.
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Current conflict document Etag.
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Operation type on which conflict occured (Put, Delete).
   */
  public ReplicationOperationTypes getOperationType() {
    return operationType;
  }

  /**
   * Operation type on which conflict occured (Put, Delete).
   * @param operationType
   */
  public void setOperationType(ReplicationOperationTypes operationType) {
    this.operationType = operationType;
  }

  /**
   * Array of conflict document Ids.
   */
  public String[] getConflicts() {
    return conflicts;
  }

  /**
   * Array of conflict document Ids.
   * @param conflicts
   */
  public void setConflicts(String[] conflicts) {
    this.conflicts = conflicts;
  }

}
