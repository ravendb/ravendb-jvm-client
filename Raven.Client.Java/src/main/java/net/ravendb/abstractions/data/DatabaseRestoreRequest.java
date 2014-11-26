package net.ravendb.abstractions.data;


public class DatabaseRestoreRequest {

  private String backupLocation;

  private String databaseLocation;

  private boolean disableReplicationDestinations;

  private String databaseName;

  private String journalsLocation;

  private String indexesLocation;

  private boolean defrag;

  private Integer restoreStartTimeout;

  public boolean isDisableReplicationDestinations() {
    return disableReplicationDestinations;
  }

  public void setDisableReplicationDestinations(boolean disableReplicationDestinations) {
    this.disableReplicationDestinations = disableReplicationDestinations;
  }

  public Integer getRestoreStartTimeout() {
    return restoreStartTimeout;
  }

  public void setRestoreStartTimeout(Integer restoreStartTimeout) {
    this.restoreStartTimeout = restoreStartTimeout;
  }

  public String getBackupLocation() {
    return backupLocation;
  }

  public void setBackupLocation(String backupLocation) {
    this.backupLocation = backupLocation;
  }

  public String getDatabaseLocation() {
    return databaseLocation;
  }

  public void setDatabaseLocation(String databaseLocation) {
    this.databaseLocation = databaseLocation;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getJournalsLocation() {
    return journalsLocation;
  }

  public void setJournalsLocation(String journalsLocation) {
    this.journalsLocation = journalsLocation;
  }

  public String getIndexesLocation() {
    return indexesLocation;
  }

  public void setIndexesLocation(String indexesLocation) {
    this.indexesLocation = indexesLocation;
  }

  public boolean isDefrag() {
    return defrag;
  }

  public void setDefrag(boolean defrag) {
    this.defrag = defrag;
  }

}
