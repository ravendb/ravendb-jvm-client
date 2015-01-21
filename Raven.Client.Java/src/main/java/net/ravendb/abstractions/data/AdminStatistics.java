package net.ravendb.abstractions.data;

import java.util.List;

public class AdminStatistics {
  private String serverName;
  private int totalNumberOfRequests;
  private String uptime;
  private AdminMemoryStatistics memory;
  private List<LoadedDatabaseStatistics> loadedDatabases;
  private List<FileSystemStats> loadedFileSystems;

  /**
   * List of loaded filesystems with their statistics.
   */
  public List<FileSystemStats> getLoadedFileSystems() {
    return loadedFileSystems;
  }

  /**
   * List of loaded filesystems with their statistics.
   * @param loadedFileSystems
   */
  public void setLoadedFileSystems(List<FileSystemStats> loadedFileSystems) {
    this.loadedFileSystems = loadedFileSystems;
  }

  /**
   * Name of a server.
   */
  public String getServerName() {
    return serverName;
  }

  /**
   * Name of a server.
   * @param serverName
   */
  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /**
   * Total number of requests received since server startup.
   */
  public int getTotalNumberOfRequests() {
    return totalNumberOfRequests;
  }

  /**
   * Total number of requests received since server startup.
   * @param totalNumberOfRequests
   */
  public void setTotalNumberOfRequests(int totalNumberOfRequests) {
    this.totalNumberOfRequests = totalNumberOfRequests;
  }

  /**
   * Server uptime.
   */
  public String getUptime() {
    return uptime;
  }

  /**
   * Server uptime.
   * @param uptime
   */
  public void setUptime(String uptime) {
    this.uptime = uptime;
  }

  /**
   * Current memory statistics.
   */
  public AdminMemoryStatistics getMemory() {
    return memory;
  }

  /**
   * Current memory statistics.
   * @param memory
   */
  public void setMemory(AdminMemoryStatistics memory) {
    this.memory = memory;
  }

  /**
   * List of loaded databases with their statistics.
   */
  public List<LoadedDatabaseStatistics> getLoadedDatabases() {
    return loadedDatabases;
  }

  /**
   * List of loaded databases with their statistics.
   * @param loadedDatabases
   */
  public void setLoadedDatabases(List<LoadedDatabaseStatistics> loadedDatabases) {
    this.loadedDatabases = loadedDatabases;
  }

}
