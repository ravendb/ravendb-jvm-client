package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseRestoringException extends RavenException {
    public DatabaseRestoringException() {
    }

    public DatabaseRestoringException(String message) {
        super(message);
    }

    public DatabaseRestoringException(String message, Throwable cause) {
        super(message, cause);
    }
}
