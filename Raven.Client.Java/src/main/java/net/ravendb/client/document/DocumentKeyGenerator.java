package net.ravendb.client.document;

import net.ravendb.client.connection.IDatabaseCommands;

public interface DocumentKeyGenerator {
  /**
   * Generates document key
   * @param dbName
   * @param dbCommands
   * @param entity
   */
  public String generate(String dbName, IDatabaseCommands dbCommands, Object entity);
}
