package net.ravendb.client.exceptions.documents.subscriptions;


public class SubscriptionClosedException extends SubscriptionException {
    private boolean canReconnect;
    private boolean noDocsLeft;

    public SubscriptionClosedException(String message) {
        super(message);
    }

    public SubscriptionClosedException(String message, boolean canReconnect) {
        super(message);

        this.canReconnect = canReconnect;
    }

    public SubscriptionClosedException(String message, boolean canReconnect, Exception inner) {
        super(message, inner);

        this.canReconnect = canReconnect;
    }

    public SubscriptionClosedException(String message, boolean canReconnect, boolean noDocsLeft) {
        super(message);
        this.canReconnect = canReconnect;
        this.noDocsLeft = noDocsLeft;
    }

    public SubscriptionClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public boolean isCanReconnect() {
        return canReconnect;
    }

    public void setCanReconnect(boolean canReconnect) {
        this.canReconnect = canReconnect;
    }

    public boolean isNoDocsLeft() {
        return noDocsLeft;
    }

    public void setNoDocsLeft(boolean noDocsLeft) {
        this.noDocsLeft = noDocsLeft;
    }
}

