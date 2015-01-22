package net.ravendb.abstractions.data;

public class AdminMemoryStatistics {

  private double databaseCacheSizeInMB;
  private double managedMemorySizeInMB;
  private double totalProcessMemorySizeInMB;

  /**
   * Size of database cache in megabytes.
   */
  public double getDatabaseCacheSizeInMB() {
    return databaseCacheSizeInMB;
  }

  /**
   * Size of database cache in megabytes.
   * @param databaseCacheSizeInMB
   */
  public void setDatabaseCacheSizeInMB(double databaseCacheSizeInMB) {
    this.databaseCacheSizeInMB = databaseCacheSizeInMB;
  }

  /**
   * Size (in megabytes) of managed memory held by server.
   */
  public double getManagedMemorySizeInMB() {
    return managedMemorySizeInMB;
  }

  /**
   * Size (in megabytes) of managed memory held by server.
   * @param managedMemorySizeInMB
   */
  public void setManagedMemorySizeInMB(double managedMemorySizeInMB) {
    this.managedMemorySizeInMB = managedMemorySizeInMB;
  }

  /**
   * Total size of memory held by server.
   */
  public double getTotalProcessMemorySizeInMB() {
    return totalProcessMemorySizeInMB;
  }

  /**
   * Total size of memory held by server.
   * @param totalProcessMemorySizeInMB
   */
  public void setTotalProcessMemorySizeInMB(double totalProcessMemorySizeInMB) {
    this.totalProcessMemorySizeInMB = totalProcessMemorySizeInMB;
  }

}
