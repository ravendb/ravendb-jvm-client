package net.ravendb.abstractions.data;

import java.util.Map;
import java.util.TreeMap;

import org.codehaus.jackson.annotate.JsonIgnore;

public class GetRequest {
  private String url;
  private Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private String query;

  public GetRequest() {
    //empty by design
  }

  public GetRequest(String url) {
    this.url = url;
  }

  public GetRequest(String url, String query) {
    this.url = url;
    this.query = query;
  }

  /**
   * Request headers.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Query information e.g. "?pageStart=10&pageSize=20".
   */
  public String getQuery() {
    return query;
  }

  /**
   * Request url (relative).
   */
  public String getUrl() {
    return url;
  }

  /**
   * Request headers.
   * @param headers
   */
  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  /**
   * Query information e.g. "?pageStart=10&pageSize=20".
   * @param query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Request url (relative).
   * @param url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Concatenated Url and Query.
   */
  @JsonIgnore
  public String getUrlAndQuery() {
    if (query == null) {
      return url;
    }
    if (query.startsWith("?")) {
      return url + query;
    }
    return url + "?" + query;
  }

}
