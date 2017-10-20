package net.ravendb.client.exceptions;

public class DatabaseDoesNotExistException extends RuntimeException {
    public DatabaseDoesNotExistException() {
    }

    public DatabaseDoesNotExistException(String message) {
        super(message);
    }

    public DatabaseDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
