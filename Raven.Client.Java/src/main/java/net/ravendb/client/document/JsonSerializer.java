package net.ravendb.client.document;

import java.io.IOException;

import net.ravendb.abstractions.exceptions.JsonReaderException;
import net.ravendb.abstractions.exceptions.JsonWriterException;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.json.linq.RavenJTokenWriter;

import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;


public class JsonSerializer {
  private static ObjectMapper objectMapper;
  private DocumentConvention convention;

  public JsonSerializer() {
    if (objectMapper == null) {
      synchronized (JsonSerializer.class) {
        if (objectMapper == null) {
          objectMapper = JsonExtensions.createDefaultJsonSerializer();
        }
      }
    }
  }

  public JsonSerializer(DocumentConvention convention) {
    this();
    this.convention = convention;
    config();
  }

  public void config() {
    if (convention.isSaveEnumsAsIntegers()) {
      objectMapper.enable(Feature.WRITE_ENUMS_USING_INDEX);
    } else {
      objectMapper.disable(Feature.WRITE_ENUMS_USING_INDEX);
    }
  }

  public void serialize(RavenJTokenWriter jsonWriter, Object value) {
    try {
      objectMapper.writeValue(jsonWriter, value);
    } catch (IOException e) {
      throw new JsonWriterException(e.getMessage(), e);
    }
  }

  public String serializeAsString(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException e) {
      throw new JsonWriterException(e.getMessage(), e);
    }
  }

  public <T> T deserialize(String input, Class<T> type) {
    try {
      return objectMapper.readValue(input, type);
    } catch (IOException e) {
      throw new JsonReaderException(e.getMessage(), e);
    }
  }

  public <T> T deserialize(RavenJToken y, Class<T> type) {
    return deserialize(y.toString(), type);
  }

  public void registerModule(Module setupMoneyModule) {
    objectMapper.registerModule(setupMoneyModule);
  }

}
