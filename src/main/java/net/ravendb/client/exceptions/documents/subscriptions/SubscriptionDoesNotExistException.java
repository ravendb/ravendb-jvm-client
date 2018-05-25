package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionDoesNotExistException extends SubscriptionException {
    public SubscriptionDoesNotExistException(String message) {
        super(message);
    }

    public SubscriptionDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
