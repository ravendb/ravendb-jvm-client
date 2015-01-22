package net.ravendb.client.connection;

import net.ravendb.abstractions.json.linq.RavenJObject;

public interface IAdminDatabaseCommands {
  /**
   *  Disables all indexing
   */
  public void stopIndexing();

  /**
   * Enables indexing
   */
  public void startIndexing();

  /**
   * Enables indexing
   * @param maxNumberOfParallelIndexTasks If set then maximum number of parallel indexing tasks will be set to this value.
   */
  public void startIndexing(Integer maxNumberOfParallelIndexTasks);

  /**
   *  Get the indexing status
   */
  public String getIndexingStatus();

  /**
   * Gets configuration for current database.
   */
  public RavenJObject getDatabaseConfiguration();
}
