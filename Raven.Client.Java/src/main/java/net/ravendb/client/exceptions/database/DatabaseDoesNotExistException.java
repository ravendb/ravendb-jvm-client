package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseDoesNotExistException extends RavenException {
    public DatabaseDoesNotExistException() {
    }

    public DatabaseDoesNotExistException(String message) {
        super(message);
    }

    public DatabaseDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
