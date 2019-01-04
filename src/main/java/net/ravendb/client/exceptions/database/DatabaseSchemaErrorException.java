package net.ravendb.client.exceptions.database;

import net.ravendb.client.exceptions.RavenException;

public class DatabaseSchemaErrorException extends RavenException {
    public DatabaseSchemaErrorException() {
    }

    public DatabaseSchemaErrorException(String message) {
        super(message);
    }

    public DatabaseSchemaErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
