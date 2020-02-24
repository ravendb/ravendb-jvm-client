package net.ravendb.client.exceptions;

public class RequestedNodeUnavailableException extends RavenException {
    public RequestedNodeUnavailableException() {
    }

    public RequestedNodeUnavailableException(String message) {
        super(message);
    }

    public RequestedNodeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
