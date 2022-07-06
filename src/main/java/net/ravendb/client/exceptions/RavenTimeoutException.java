package net.ravendb.client.exceptions;

public class RavenTimeoutException extends RavenException {
    public RavenTimeoutException() {
    }

    public RavenTimeoutException(String message) {
        super(message);
    }

    public RavenTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
