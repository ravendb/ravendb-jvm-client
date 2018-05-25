package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionInvalidStateException extends SubscriptionException {
    public SubscriptionInvalidStateException(String message) {
        super(message);
    }

    public SubscriptionInvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
