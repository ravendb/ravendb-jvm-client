package net.ravendb.abstractions.data;


public class StorageStats {
  private VoronStorageStats voronStats;
  private EsentStorageStats esentStats;

  public VoronStorageStats getVoronStats() {
    return voronStats;
  }

  public void setVoronStats(VoronStorageStats voronStats) {
    this.voronStats = voronStats;
  }

  public EsentStorageStats getEsentStats() {
    return esentStats;
  }

  public void setEsentStats(EsentStorageStats esentStats) {
    this.esentStats = esentStats;
  }

}
