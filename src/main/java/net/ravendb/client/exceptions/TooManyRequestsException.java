package net.ravendb.client.exceptions;

public class TooManyRequestsException extends UnsuccessfulAiRequestException {

    public TooManyRequestsException(String message) {
        super(message, 429);
    }
}
