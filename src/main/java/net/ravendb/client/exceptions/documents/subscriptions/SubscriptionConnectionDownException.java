package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionConnectionDownException extends SubscriptionException {
    public SubscriptionConnectionDownException(String message) {
        super(message);
    }

    public SubscriptionConnectionDownException(String message, Throwable cause) {
        super(message, cause);
    }
}
