package net.ravendb.client.document;

import java.io.IOException;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Action3;
import net.ravendb.abstractions.json.linq.RavenJToken;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.DeserializationProblemHandler;
import org.codehaus.jackson.map.JsonDeserializer;


public class DefaultRavenContractResolver extends DeserializationProblemHandler  {

  protected ThreadLocal<Action3<Object, String, RavenJToken>> currentExtensionData = new ThreadLocal<>();

  public CleanCloseable registerForExtensionData(Action3<Object, String, RavenJToken> action) {
    if (currentExtensionData.get() != null) {
      throw new IllegalStateException("Cannot add a data setter becuase on is already added.");
    }
    currentExtensionData.set(action);
    return new ClearExtensionData();
  }

  public class ClearExtensionData implements CleanCloseable {
    @Override
    public void close() {
      currentExtensionData.set(null);
    }
  }

  @Override
  public boolean handleUnknownProperty(DeserializationContext ctxt, JsonDeserializer< ? > deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
    if (currentExtensionData.get() != null) {
      currentExtensionData.get().apply(beanOrClass, propertyName, RavenJToken.readFrom(ctxt.getParser()));
    }
    return true;
  }

}
