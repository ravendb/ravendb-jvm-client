package net.ravendb.client.exceptions;

public final class InsufficientQuotaException extends TooManyRequestsException {

    public InsufficientQuotaException(String message) {
        super(message);
    }
}
