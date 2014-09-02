package net.ravendb.abstractions.data;


public class VoronActiveTransaction {
  private long id;
  private String flags;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getFlags() {
    return flags;
  }

  public void setFlags(String flags) {
    this.flags = flags;
  }

}
