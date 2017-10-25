package net.ravendb.client.exceptions.cluster;

import net.ravendb.client.exceptions.RavenException;

public class NoLeaderException extends RavenException {
    public NoLeaderException() {
    }

    public NoLeaderException(String message) {
        super(message);
    }

    public NoLeaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
