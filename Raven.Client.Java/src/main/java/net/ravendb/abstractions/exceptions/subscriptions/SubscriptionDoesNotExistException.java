package net.ravendb.abstractions.exceptions.subscriptions;

import org.apache.http.HttpStatus;


public class SubscriptionDoesNotExistException extends SubscriptionException {
  public static int RELEVANT_HTTP_STATUS_CODE = HttpStatus.SC_NOT_FOUND;

  public SubscriptionDoesNotExistException() {
    super(RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionDoesNotExistException(String message) {
    super(message, RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionDoesNotExistException(String message, Throwable cause) {
    super(message, cause, RELEVANT_HTTP_STATUS_CODE);
  }
}
