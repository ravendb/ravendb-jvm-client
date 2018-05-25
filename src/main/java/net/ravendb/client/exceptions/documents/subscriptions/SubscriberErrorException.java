package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriberErrorException extends SubscriptionException {
    public SubscriberErrorException(String message) {
        super(message);
    }

    public SubscriberErrorException(String message, Throwable cause) {
        super(message, cause);
    }

}
