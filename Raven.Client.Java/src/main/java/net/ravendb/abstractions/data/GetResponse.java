package net.ravendb.abstractions.data;

import java.util.Map;
import java.util.TreeMap;

import net.ravendb.abstractions.json.linq.RavenJToken;


public class GetResponse {
  private RavenJToken result;
  private Map<String, String> headers;
  private int status;
  private boolean forceRetry;

  public GetResponse() {
    headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  /**
   * Indicates if request should be retried (forced).
   */
  public boolean isForceRetry() {
    return forceRetry;
  }

  /**
   * Indicates if request should be retried (forced).
   * @param forceRetry
   */
  public void setForceRetry(boolean forceRetry) {
    this.forceRetry = forceRetry;
  }

  /**
   * Response headers.
   */
  public Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Response result as JSON.
   */
  public RavenJToken getResult() {
    return result;
  }

  /**
   * Response HTTP status code.
   */
  public int getStatus() {
    return status;
  }

  /**
   * Response headers.
   * @param headers
   */
  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  /**
   * Response result as JSON.
   * @param result
   */
  public void setResult(RavenJToken result) {
    this.result = result;
  }

  /**
   * Response HTTP status code.
   * @param status
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * Method used to check if request has errors.
   * Returns:
   * - false - if Status is 0, 200, 201, 203, 204, 304 and 404
   * - true - otherwise
   * @return false if Status is 0, 200, 201, 203, 204, 304 and 404. True otherwise.
   */
  public boolean isRequestHasErrors() {
    switch (status) {
    case 0: // aggressively cached
    case  200: //known non error value
    case 201:
    case 203:
    case 204:
    case 304:
    case 404:
      return false;
    default:
      return true;
    }
  }


}
