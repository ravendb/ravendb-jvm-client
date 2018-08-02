package net.ravendb.client.exceptions;

public class ClientVersionMismatchException extends RavenException {
    public ClientVersionMismatchException() {
    }

    public ClientVersionMismatchException(String message) {
        super(message);
    }

    public ClientVersionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
