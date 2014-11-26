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
   */
  public void startIndexing(Integer maxNumberOfParallelIndexTasks);

  /**
   *  Get the indexing status
   */
  public String getIndexingStatus();

  public RavenJObject getDatabaseConfiguration();
}
