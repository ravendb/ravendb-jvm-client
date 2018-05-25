package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionDoesNotBelongToNodeException extends SubscriptionException {
    public String appropriateNode;

    public SubscriptionDoesNotBelongToNodeException(String message) {
        super(message);
    }

    public SubscriptionDoesNotBelongToNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
