package net.ravendb.client.exceptions.documents.subscriptions;

public class DocumentUnderActiveMigrationException extends SubscriptionException {
    public DocumentUnderActiveMigrationException(String message) {
        super(message);
    }

    public DocumentUnderActiveMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
