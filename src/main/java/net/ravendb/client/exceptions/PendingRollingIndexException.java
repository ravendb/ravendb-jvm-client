package net.ravendb.client.exceptions;

public class PendingRollingIndexException extends RavenException {
    public PendingRollingIndexException() {
    }

    public PendingRollingIndexException(String message) {
        super(message);
    }

    public PendingRollingIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
