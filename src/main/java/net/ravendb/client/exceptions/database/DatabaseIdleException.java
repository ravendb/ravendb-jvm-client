package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseIdleException extends RavenException {
    public DatabaseIdleException() {
    }

    public DatabaseIdleException(String message) {
        super(message);
    }

    public DatabaseIdleException(String message, Throwable cause) {
        super(message, cause);
    }
}
