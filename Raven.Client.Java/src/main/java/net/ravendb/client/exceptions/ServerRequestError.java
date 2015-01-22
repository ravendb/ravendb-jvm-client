package net.ravendb.client.exceptions;


public class ServerRequestError {
  private String url;
  private String error;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

}
