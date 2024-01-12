package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionCreationException extends SubscriptionException {
    public SubscriptionCreationException(String message) {
        super(message);
    }

    public SubscriptionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
