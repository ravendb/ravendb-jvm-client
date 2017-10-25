package net.ravendb.client.exceptions;

public class InvalidQueryException extends RavenException {
    public InvalidQueryException() {
    }

    public InvalidQueryException(String message) {
        super(message);
    }

    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
