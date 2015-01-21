package net.ravendb.abstractions.data;

import java.util.UUID;

public class BulkInsertChangeNotification extends DocumentChangeNotification {
  private UUID operationId;

  /**
   * BulkInsert operation Id.
   */
  public UUID getOperationId() {
    return operationId;
  }

  /**
   * BulkInsert operation Id.
   * @param operationId
   */
  public void setOperationId(UUID operationId) {
    this.operationId = operationId;
  }

}
