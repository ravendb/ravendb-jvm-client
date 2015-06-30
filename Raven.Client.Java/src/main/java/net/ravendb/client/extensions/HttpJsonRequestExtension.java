package net.ravendb.client.extensions;

import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.Map;


public class HttpJsonRequestExtension {
  public static void assertNotFailingResponse(CloseableHttpResponse response) {
    if (response.getStatusLine().getStatusCode() < 300) {
      return;
    }

    try {
      String error = IOUtils.toString(response.getEntity().getContent(), "UTF-8");

      RavenJObject ravenJObject = RavenJObject.parse(error);
      if (ravenJObject.containsKey("Error")) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RavenJToken> prop : ravenJObject) {
          if ("Error".equals(prop.getKey())){
            continue;
          }
          sb.append(prop.getKey()).append(": ").append(prop.getValue().toString()).append("\n");
        }

        if (sb.length() > 0) {
          sb.append("\n");
        }
        sb.append(ravenJObject.value(String.class, "Error"));

        throw new ErrorResponseException(response, sb.toString());
      }

    } catch (Exception e) {
      throw new ErrorResponseException(response, e.getMessage(), e);
    }
  }
}
