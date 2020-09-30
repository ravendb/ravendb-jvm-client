package net.ravendb.client.exceptions.documents.subscriptions;


public class SubscriptionClosedException extends SubscriptionException {
    private boolean canReconnect;

    public SubscriptionClosedException(String message) {
        super(message);
    }

    public SubscriptionClosedException(String message, boolean canReconnect) {
        super(message);

        this.canReconnect = canReconnect;
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
}

