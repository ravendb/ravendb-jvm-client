package net.ravendb.abstractions.exceptions.subscriptions;

public class SubscriptionClosedException extends SubscriptionException {

  public static int RELEVANT_HTTP_STATUS_CODE = 306; // HTTP unused

  public SubscriptionClosedException() {
    super(RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionClosedException(String message) {
    super(message, RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionClosedException(String message, Throwable cause) {
    super(message, cause, RELEVANT_HTTP_STATUS_CODE);
  }

}
