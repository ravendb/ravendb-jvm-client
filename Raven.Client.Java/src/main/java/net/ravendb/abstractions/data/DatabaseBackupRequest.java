package net.ravendb.abstractions.data;


public class DatabaseBackupRequest {
  private String backupLocation;

  private DatabaseDocument databaseDocument;

  /**
   * Path to directory where backup should lie (must be accessible from server).
   */
  public String getBackupLocation() {
    return backupLocation;
  }

  /**
   * Path to directory where backup should lie (must be accessible from server).
   * @param backupLocation
   */
  public void setBackupLocation(String backupLocation) {
    this.backupLocation = backupLocation;
  }

  /**
   * DatabaseDocument that will be inserted with backup. If null then document will be taken from server.
   */
  public DatabaseDocument getDatabaseDocument() {
    return databaseDocument;
  }

  /**
   * DatabaseDocument that will be inserted with backup. If null then document will be taken from server.
   * @param databaseDocument
   */
  public void setDatabaseDocument(DatabaseDocument databaseDocument) {
    this.databaseDocument = databaseDocument;
  }

}
