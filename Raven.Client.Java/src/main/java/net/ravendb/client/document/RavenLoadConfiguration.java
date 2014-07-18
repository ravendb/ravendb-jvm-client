package net.ravendb.client.document;

import java.util.HashMap;
import java.util.Map;

import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.ILoadConfiguration;


public class RavenLoadConfiguration implements ILoadConfiguration {
  private Map<String, RavenJToken> transformerParameters = new HashMap<>();

  public Map<String, RavenJToken> getTransformerParameters() {
    return transformerParameters;
  }

  public void setTransformerParameters(Map<String, RavenJToken> transformerParameters) {
    this.transformerParameters = transformerParameters;
  }

  @Override
  public void addTransformerParameter(String name, RavenJToken value) {
    transformerParameters.put(name, value);
  }



}
