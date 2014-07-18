package net.ravendb.client.connection;

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
   * @return
   */
  public String getIndexingStatus();

}
