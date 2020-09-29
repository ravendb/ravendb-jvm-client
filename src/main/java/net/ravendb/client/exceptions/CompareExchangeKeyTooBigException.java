package net.ravendb.client.exceptions;

public class CompareExchangeKeyTooBigException extends RavenException {

    public CompareExchangeKeyTooBigException() {
    }

    public CompareExchangeKeyTooBigException(String message) {
        super(message);
    }

    public CompareExchangeKeyTooBigException(String message, Throwable cause) {
        super(message, cause);
    }
}
