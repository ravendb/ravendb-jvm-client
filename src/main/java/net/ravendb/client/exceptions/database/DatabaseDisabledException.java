package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseDisabledException extends RavenException {
    public DatabaseDisabledException() {
    }

    public DatabaseDisabledException(String message) {
        super(message);
    }

    public DatabaseDisabledException(String message, Throwable cause) {
        super(message, cause);
    }
}
