package net.ravendb.client;

import net.ravendb.abstractions.json.linq.RavenJToken;

public interface ILoadConfiguration {
  void addTransformerParameter(String name, RavenJToken value);
}
