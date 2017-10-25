package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseLoadFailureException extends RavenException {
    public DatabaseLoadFailureException() {
    }

    public DatabaseLoadFailureException(String message) {
        super(message);
    }

    public DatabaseLoadFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
