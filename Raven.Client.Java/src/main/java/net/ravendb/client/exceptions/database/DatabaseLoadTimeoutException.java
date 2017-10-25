package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseLoadTimeoutException extends RavenException {
    public DatabaseLoadTimeoutException() {
    }

    public DatabaseLoadTimeoutException(String message) {
        super(message);
    }

    public DatabaseLoadTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
