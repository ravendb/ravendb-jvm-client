package net.ravendb.abstractions.connection;

import java.io.IOException;

import net.ravendb.abstractions.data.Etag;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;


public class ErrorResponseException extends RuntimeException {

  private CloseableHttpResponse response;
  private String responseString;

  public ErrorResponseException(ErrorResponseException e, String message) {
    super(message);
    response = e.response;
    responseString = e.responseString;
  }

  public ErrorResponseException(CloseableHttpResponse response, String msg) {
    super(msg);
    this.response = response;
  }

  public ErrorResponseException(CloseableHttpResponse response, String msg, Throwable cause) {
    super(msg, cause);
    this.response = response;
  }

  public ErrorResponseException(CloseableHttpResponse response, String msg, String responseString) {
    super(msg);
    this.response = response;
    this.responseString = responseString;
  }

  public int getStatusCode() {
    return response.getStatusLine().getStatusCode();
  }

  public static ErrorResponseException fromResponseMessage(CloseableHttpResponse response) {
    return fromResponseMessage(response, true);
  }

  public static ErrorResponseException fromResponseMessage(CloseableHttpResponse response, boolean readErrorString) {
    StringBuilder sb = new StringBuilder("Status code :").append(response.getStatusLine().getStatusCode()).append("\n");

    String responseString = null;
    if (readErrorString && response.getEntity() != null) {
      try {
        responseString = IOUtils.toString(response.getEntity().getContent());
        sb.append(responseString);
      } catch (IOException e) {
        sb.append(e.getMessage());
      }
    }

    ErrorResponseException ex = new ErrorResponseException(response, sb.toString());
    ex.responseString = responseString;
    return ex;
  }


  public CloseableHttpResponse getResponse() {
    return response;
  }

  public String getResponseString() {
    return responseString;
  }

  public Etag getEtag() {
    if (response.getFirstHeader("ETag") == null) {
      return null;
    }
    String responseHeader = response.getFirstHeader("ETag").getValue();
    if (responseHeader.startsWith("\"")) {
      return Etag.parse(responseHeader.substring(1, responseHeader.length() - 1));
    }
    return Etag.parse(responseHeader);

  }

}
