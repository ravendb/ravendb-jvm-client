package net.ravendb.client.exceptions;

public class ReplicationHubNotFoundException extends RavenException {
    public ReplicationHubNotFoundException() {
    }

    public ReplicationHubNotFoundException(String message) {
        super(message);
    }

    public ReplicationHubNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
