package net.ravendb.client.exceptions.server;

import net.ravendb.client.exceptions.RavenException;

public class ServerLoadFailureException extends RavenException {
    public ServerLoadFailureException() {
    }

    public ServerLoadFailureException(String message) {
        super(message);
    }

    public ServerLoadFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
