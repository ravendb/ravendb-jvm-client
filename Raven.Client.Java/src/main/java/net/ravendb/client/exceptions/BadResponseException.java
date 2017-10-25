package net.ravendb.client.exceptions;

public class BadResponseException extends RavenException {
    public BadResponseException() {
    }

    public BadResponseException(String message) {
        super(message);
    }

    public BadResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
