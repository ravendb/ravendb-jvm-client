package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseConcurrentLoadTimeoutException extends RavenException {
    public DatabaseConcurrentLoadTimeoutException() {
    }

    public DatabaseConcurrentLoadTimeoutException(String message) {
        super(message);
    }

    public DatabaseConcurrentLoadTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
