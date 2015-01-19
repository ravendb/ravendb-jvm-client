package net.ravendb.abstractions.exceptions.subscriptions;

import org.apache.http.HttpStatus;


public class SubscriptionInUseException extends SubscriptionException {
  public static int RELEVANT_HTTP_STATUS_CODE = HttpStatus.SC_GONE;

  public SubscriptionInUseException() {
    super(RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionInUseException(String message) {
    super(message, RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionInUseException(String message, Throwable cause) {
    super(message, cause, RELEVANT_HTTP_STATUS_CODE);
  }
}
