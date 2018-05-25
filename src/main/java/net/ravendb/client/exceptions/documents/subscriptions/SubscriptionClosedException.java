package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionClosedException extends SubscriptionException {
    public SubscriptionClosedException(String message) {
        super(message);
    }

    public SubscriptionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
