package net.ravendb.abstractions.data;


public class ExtensionsLog {
  private String name;
  private ExtensionsLogDetail[] installed;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ExtensionsLogDetail[] getInstalled() {
    return installed;
  }

  public void setInstalled(ExtensionsLogDetail[] installed) {
    this.installed = installed;
  }

}
