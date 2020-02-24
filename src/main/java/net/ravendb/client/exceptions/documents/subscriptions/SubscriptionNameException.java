package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionNameException extends SubscriptionException {

    public SubscriptionNameException(String message) {
        super(message);
    }

    public SubscriptionNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
