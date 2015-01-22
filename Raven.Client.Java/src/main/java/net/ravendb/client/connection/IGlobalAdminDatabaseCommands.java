package net.ravendb.client.connection;

import net.ravendb.abstractions.data.AdminStatistics;
import net.ravendb.abstractions.data.BuildNumber;
import net.ravendb.abstractions.data.DatabaseDocument;
import net.ravendb.abstractions.data.DatabaseRestoreRequest;


public interface IGlobalAdminDatabaseCommands {

  /**
   * Gets the build number
   */
  public BuildNumber getBuildNumber();

  /**
   * Returns the names of all tenant databases on the RavenDB server
   * @param pageSize
   */
  public String[] getDatabaseNames(int pageSize);

  /**
   * Returns the names of all tenant databases on the RavenDB server
   * @param pageSize
   * @param start
   */
  public String[] getDatabaseNames(int pageSize, int start);

  /**
   * Retrieve the statistics for the database
   */
  public AdminStatistics getStatistics();

  /**
   * Creates a database
   * @param databaseDocument
   */
  public void createDatabase(DatabaseDocument databaseDocument);

  /**
   * Used to delete a database from a server, with a possibility to remove all the data from hard drive.
   * Warning: if hardDelete is set to <c>true</c> then ALL data will be removed from the data directory of a database.
   * @param dbName Name of a database to delete
   */
  public void deleteDatabase(String dbName);

  /**
   * Used to delete a database from a server, with a possibility to remove all the data from hard drive.
   * Warning: if hardDelete is set to true then ALL data will be removed from the data directory of a database.
   * @param dbName Name of a database to delete
   * @param hardDelete Should all data be removed (data files, indexing files, etc.). Default: false
   */
  public void deleteDatabase(String dbName, boolean hardDelete);

  /**
   * Sends an async command to compact a database. During the compaction the specified database will be offline.
   * @param databaseName Name of a database to compact
   */
  public Operation compactDatabase(String databaseName);

  /**
   * Gets DatabaseCommands
   */
  public IDatabaseCommands getCommands();


  /**
   * Begins a restore operation
   * @param restoreRequest
   */
  public Operation startRestore(DatabaseRestoreRequest restoreRequest);


  /**
   * Begins a backup operation
   * @param backupLocation Path to directory where backup will be stored
   * @param databaseDocument Database configuration document that will be stored with backup in 'Database.Document'
   * file. Pass null to use the one from system database. WARNING: Database configuration document may contain
   * sensitive data which will be decrypted and stored in backup.
   * @param incremental Indicates if backup is incremental
   * @param databaseName Name of a database that will be backed up
   */
  public void startBackup(String backupLocation, DatabaseDocument databaseDocument, boolean incremental, String databaseName);

  /**
   * Ensures that the database exists, creating it if needed
   * @param name
   */
  public void ensureDatabaseExists(String name);

  /**
   * Ensures that the database exists, creating it if needed
   * @param name
   * @param ignoreFailures
   */
  public void ensureDatabaseExists(String name, boolean ignoreFailures);

}
