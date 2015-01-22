package net.ravendb.abstractions.extensions;

import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.imports.json.JsonConvert;

import org.apache.commons.lang.StringUtils;


public class ExceptionExtensions {

  public static <T> T tryReadErrorResponseObject(Class<T> clazz, ErrorResponseException ex) {
    String response = ex.getResponseString();
    if (StringUtils.isEmpty(response)) {
      return null;
    }
    try {
      return JsonConvert.deserializeObject(clazz, response);
    } catch (Exception e) {
      return null;
    }
  }

}
