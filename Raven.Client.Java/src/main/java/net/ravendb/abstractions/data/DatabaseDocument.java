package net.ravendb.abstractions.data;

import java.util.HashMap;
import java.util.Map;

public class DatabaseDocument {
  private String id;
  private Map<String, String> settings;
  private Map<String, String> securedSettings;
  private boolean disabled;

  /**
   * The ID of a database. Can be either the database name ("Northwind") or the full document name ("Raven/Databases/Northwind").
   */
  public String getId() {
    return id;
  }

  /**
   * The ID of a database. Can be either the database name ("Northwind") or the full document name ("Raven/Databases/Northwind").
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Database settings (unsecured).
   */
  public Map<String, String> getSettings() {
    return settings;
  }

  /**
   * Database settings (unsecured).
   * @param settings
   */
  public void setSettings(Map<String, String> settings) {
    this.settings = settings;
  }

  /**
   * Database settings (secured).
   */
  public Map<String, String> getSecuredSettings() {
    return securedSettings;
  }

  /**
   * Database settings (secured).
   * @param securedSettings
   */
  public void setSecuredSettings(Map<String, String> securedSettings) {
    this.securedSettings = securedSettings;
  }

  /**
   * Indicates if database is disabled or not.
   */
  public boolean isDisabled() {
    return disabled;
  }

  /**
   * Indicates if database is disabled or not.
   * @param disabled
   */
  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }


  public DatabaseDocument() {
    settings = new HashMap<>();
    securedSettings = new HashMap<>();
  }
}
