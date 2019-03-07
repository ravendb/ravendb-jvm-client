package net.ravendb.client.exceptions;

public class UnsuccessfulRequestException extends RavenException {
    public UnsuccessfulRequestException(String message) {
        super(message + " Request to a server has failed.");
    }

    public UnsuccessfulRequestException(String message, Throwable cause) {
        super(message + "Request to a server has failed. Reason: " + cause.getMessage(), cause);
    }
}
