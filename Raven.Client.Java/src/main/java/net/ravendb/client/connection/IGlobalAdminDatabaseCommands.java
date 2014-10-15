package net.ravendb.client.connection;

import net.ravendb.abstractions.data.AdminStatistics;
import net.ravendb.abstractions.data.BuildNumber;
import net.ravendb.abstractions.data.DatabaseDocument;
import net.ravendb.abstractions.data.DatabaseRestoreRequest;


public interface IGlobalAdminDatabaseCommands {

  /**
   * Gets the build number
   * @return
   */
  public BuildNumber getBuildNumber();
  /**
   * Returns the names of all tenant databases on the RavenDB server
   * @param pageSize
   * @return
   */
  public String[] getDatabaseNames(int pageSize);

  /**
   * Returns the names of all tenant databases on the RavenDB server
   * @param pageSize
   * @param start
   * @return
   */
  public String[] getDatabaseNames(int pageSize, int start);

  /**
   * Get admin statistics
   * @return
   */
  public AdminStatistics getStatistics();

  /**
   * Creates a database
   * @param databaseDocument
   */
  public void createDatabase(DatabaseDocument databaseDocument);

  /**
   * Deteles a database with the specified name
   * @param dbName
   */
  public void deleteDatabase(String dbName);

  /**
   * Deteles a database with the specified name
   * @param dbName
   * @param hardDelete
   */
  public void deleteDatabase(String dbName, boolean hardDelete);

  /**
   * Sends an async command to compact a database. During the compaction the specified database will be offline.
   * @param databaseName
   */
  public void compactDatabase(String databaseName);

  /**
   * Gets DatabaseCommands
   * @return
   */
  public IDatabaseCommands getCommands();


  /**
   * Begins a restore operation
   * @param restoreLocation
   * @param databaseLocation
   */
  public Operation startRestore(DatabaseRestoreRequest restoreRequest);


  /**
   * Begins a backup operation
   * @param backupLocation
   * @param databaseDocument
   */
  public void startBackup(String backupLocation, DatabaseDocument databaseDocument, boolean incremental, String databaseName);

  /**
   * Ensures that the database exists, creating it if needed
   * @param name
   * @param ignoreFailures
   */
  public void ensureDatabaseExists(String name);

  /**
   * Ensures that the database exists, creating it if needed
   * @param name
   * @param ignoreFailures
   */
  public void ensureDatabaseExists(String name, boolean ignoreFailures);

}
