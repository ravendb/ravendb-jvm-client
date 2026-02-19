package net.ravendb.client.exceptions;

public final class TooManyTokensException extends TooManyRequestsException {

    public TooManyTokensException(String message) {
        super(message);
    }
}
