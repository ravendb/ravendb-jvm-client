package net.ravendb.abstractions.data;


public class DatabaseRestoreRequest {

  private String backupLocation;

  private String databaseLocation;

  private boolean disableReplicationDestinations;

  private boolean generateNewDatabaseId;

  private String databaseName;

  private String journalsLocation;

  private String indexesLocation;

  private boolean defrag;

  private Integer restoreStartTimeout;

  /**
   * Indicates if restored database should have new Id generated. By default it will be the same.
   * @return
   */
  public boolean isGenerateNewDatabaseId() {
    return generateNewDatabaseId;
  }

  /**
   * Set if restored database should have new Id generated. By default it will be the same.
   * @param generateNewDatabaseId
   */
  public void setGenerateNewDatabaseId(boolean generateNewDatabaseId) {
    this.generateNewDatabaseId = generateNewDatabaseId;
  }

  /**
   * Indicates if all replication destinations should disabled after restore (only when Replication bundle is activated).
   */
  public boolean isDisableReplicationDestinations() {
    return disableReplicationDestinations;
  }

  /**
   * Indicates if all replication destinations should disabled after restore (only when Replication bundle is activated).
   * @param disableReplicationDestinations
   */
  public void setDisableReplicationDestinations(boolean disableReplicationDestinations) {
    this.disableReplicationDestinations = disableReplicationDestinations;
  }

  /**
   * Maximum number of seconds to wait for restore to start (only one restore can run simultaneously). If exceeded, then status code 503 (Service Unavailable) will be returned.
   */
  public Integer getRestoreStartTimeout() {
    return restoreStartTimeout;
  }

  /**
   * Maximum number of seconds to wait for restore to start (only one restore can run simultaneously). If exceeded, then status code 503 (Service Unavailable) will be returned.
   */
  public void setRestoreStartTimeout(Integer restoreStartTimeout) {
    this.restoreStartTimeout = restoreStartTimeout;
  }

  /**
   * Path to directory where backup lies.
   */
  public String getBackupLocation() {
    return backupLocation;
  }

  /**
   * Path to directory where backup lies.
   * @param backupLocation
   */
  public void setBackupLocation(String backupLocation) {
    this.backupLocation = backupLocation;
  }

  /**
   * Path to the directory of a new database. If null then default location will be assumed.
   */
  public String getDatabaseLocation() {
    return databaseLocation;
  }

  /**
   * Path to the directory of a new database. If null then default location will be assumed.
   * @param databaseLocation
   */
  public void setDatabaseLocation(String databaseLocation) {
    this.databaseLocation = databaseLocation;
  }

  /**
   * Indicates what should be the name of database after restore. If null then name will be read from 'Database.Document' found in backup.
   */
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Indicates what should be the name of database after restore. If null then name will be read from 'Database.Document' found in backup.
   */
  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  /**
   * Path to directory where journals lies (if null, then default location will be assumed).
   */
  public String getJournalsLocation() {
    return journalsLocation;
  }

  /**
   * Path to directory where journals lies (if null, then default location will be assumed).
   */
  public void setJournalsLocation(String journalsLocation) {
    this.journalsLocation = journalsLocation;
  }

  /**
   * Path to directory where indexes lies (if null, then default location will be assumed).
   */
  public String getIndexesLocation() {
    return indexesLocation;
  }

  /**
   * Path to directory where indexes lies (if null, then default location will be assumed).
   */
  public void setIndexesLocation(String indexesLocation) {
    this.indexesLocation = indexesLocation;
  }

  /**
   * Indicates if defragmentation should take place after restore.
   */
  public boolean isDefrag() {
    return defrag;
  }

  /**
   * Indicates if defragmentation should take place after restore.
   */
  public void setDefrag(boolean defrag) {
    this.defrag = defrag;
  }

}
