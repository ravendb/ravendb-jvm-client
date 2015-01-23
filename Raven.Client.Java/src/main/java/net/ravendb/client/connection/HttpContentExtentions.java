package net.ravendb.client.connection;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;


public class HttpContentExtentions {

  public static void setContentType(InputStreamEntity innerEntity, Map<String, String> headers) {
    String contentType = headers.get("Content-Type");
    if (contentType == null) {
      innerEntity.setContentType(ContentType.APPLICATION_JSON.toString());
    } else if (StringUtils.isNotEmpty(contentType)) {
      innerEntity.setContentType(contentType);
    }
  }

}
