package net.ravendb.client.exceptions.security;

import net.ravendb.client.exceptions.RavenException;

public class SecurityException extends RavenException {
    public SecurityException() {
    }

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
