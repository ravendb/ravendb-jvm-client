package net.ravendb.client.extensions;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;


public class HttpJsonRequestExtension {
  public static void assertNotFailingResponse(HttpResponse response) {
    if (response.getStatusLine().getStatusCode() >= 300) {
      StringBuilder sb = new StringBuilder();
      sb.append(response.getStatusLine().getStatusCode())
        .append("\n");

      try {
        String error = IOUtils.toString(response.getEntity().getContent());
        sb.append(error);
      } catch (IOException e) {
        sb.append(e.getMessage());
      }

      throw new IllegalStateException(sb.toString());
    }
  }
}
