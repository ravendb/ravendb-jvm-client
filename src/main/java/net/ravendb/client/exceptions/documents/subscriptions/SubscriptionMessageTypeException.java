package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionMessageTypeException extends SubscriptionException {

    public SubscriptionMessageTypeException(String message) {
        super(message);
    }

    public SubscriptionMessageTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
