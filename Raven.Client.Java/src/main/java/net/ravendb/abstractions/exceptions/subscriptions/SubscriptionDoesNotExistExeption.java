package net.ravendb.abstractions.exceptions.subscriptions;

import org.apache.http.HttpStatus;


public class SubscriptionDoesNotExistExeption extends SubscriptionException {
  public static int RELEVANT_HTTP_STATUS_CODE = HttpStatus.SC_NOT_FOUND;

  public SubscriptionDoesNotExistExeption() {
    super(RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionDoesNotExistExeption(String message) {
    super(message, RELEVANT_HTTP_STATUS_CODE);
  }

  public SubscriptionDoesNotExistExeption(String message, Throwable cause) {
    super(message, cause, RELEVANT_HTTP_STATUS_CODE);
  }
}
