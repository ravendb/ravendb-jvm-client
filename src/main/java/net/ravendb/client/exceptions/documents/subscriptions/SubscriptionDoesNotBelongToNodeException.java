package net.ravendb.client.exceptions.documents.subscriptions;

public class SubscriptionDoesNotBelongToNodeException extends SubscriptionException {
    private String appropriateNode;

    public SubscriptionDoesNotBelongToNodeException(String message) {
        super(message);
    }

    public SubscriptionDoesNotBelongToNodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getAppropriateNode() {
        return appropriateNode;
    }

    public void setAppropriateNode(String appropriateNode) {
        this.appropriateNode = appropriateNode;
    }
}
