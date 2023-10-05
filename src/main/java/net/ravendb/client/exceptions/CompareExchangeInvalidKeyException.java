package net.ravendb.client.exceptions;

public class CompareExchangeInvalidKeyException extends RavenException {
    public CompareExchangeInvalidKeyException() {
    }

    public CompareExchangeInvalidKeyException(String message) {
        super(message);
    }

    public CompareExchangeInvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
