package net.ravendb.client.exceptions.cluster;

import net.ravendb.client.exceptions.RavenException;

public class CommandExecutionException extends RavenException {
    public CommandExecutionException() {
    }

    public CommandExecutionException(String message) {
        super(message);
    }

    public CommandExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
