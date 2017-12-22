package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseNotRelevantException extends RavenException {
    public DatabaseNotRelevantException() {
    }

    public DatabaseNotRelevantException(String message) {
        super(message);
    }

    public DatabaseNotRelevantException(String message, Throwable cause) {
        super(message, cause);
    }
}
