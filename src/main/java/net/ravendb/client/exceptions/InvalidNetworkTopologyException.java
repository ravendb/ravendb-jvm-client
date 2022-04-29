package net.ravendb.client.exceptions;

public class InvalidNetworkTopologyException extends RavenException {
    public InvalidNetworkTopologyException() {
    }

    public InvalidNetworkTopologyException(String message) {
        super(message);
    }

    public InvalidNetworkTopologyException(String message, Throwable cause) {
        super(message, cause);
    }
}
