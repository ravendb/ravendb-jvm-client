package net.ravendb.abstractions.exceptions.subscriptions;


public abstract class SubscriptionException extends Exception {

  private int responseStatusCode;


  public int getResponseStatusCode() {
    return responseStatusCode;
  }


  protected SubscriptionException(int httpResponseCode) {
    super();
    this.responseStatusCode = httpResponseCode;
  }

  protected SubscriptionException(String message, int httpResponseCode) {
    super(message);
    this.responseStatusCode = httpResponseCode;
  }


  protected SubscriptionException(String message, Throwable cause, int httpResponseCode) {
    super(message, cause);
    this.responseStatusCode = httpResponseCode;
  }

}
