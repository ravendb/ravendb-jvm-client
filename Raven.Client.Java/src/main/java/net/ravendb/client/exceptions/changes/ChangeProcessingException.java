package net.ravendb.client.exceptions.changes;

import net.ravendb.client.exceptions.RavenException;

public class ChangeProcessingException extends RavenException {
    public ChangeProcessingException() {
    }

    public ChangeProcessingException(String message) {
        super(message);
    }

    public ChangeProcessingException(Throwable cause) {
        super("Failed to process change.", cause);
    }
}
