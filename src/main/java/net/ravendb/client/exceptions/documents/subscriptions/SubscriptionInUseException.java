package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionInUseException extends SubscriptionException {

    public SubscriptionInUseException(String message) {
        super(message);
    }

    public SubscriptionInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
