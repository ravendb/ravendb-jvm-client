package net.ravendb.client.exceptions.documents.subscriptions;

import net.ravendb.client.exceptions.RavenException;

public abstract class SubscriptionException extends RavenException {
    protected SubscriptionException(String message) {
        super(message);
    }

    protected SubscriptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
