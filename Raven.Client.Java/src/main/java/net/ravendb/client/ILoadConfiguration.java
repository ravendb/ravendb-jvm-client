package net.ravendb.client;

import net.ravendb.abstractions.json.linq.RavenJToken;

public interface ILoadConfiguration {
  /**
   * Adds transformer parameter that will be passed to transformer on server-side.
   * @param name Name of the parameter
   * @param value Value of the parameter
   */
  void addTransformerParameter(String name, RavenJToken value);
}
