package net.ravendb.client.exceptions.documents.session;

import net.ravendb.client.exceptions.RavenException;

public class NonUniqueObjectException extends RavenException {
    public NonUniqueObjectException() {
    }

    public NonUniqueObjectException(String message) {
        super(message);
    }

    public NonUniqueObjectException(String message, Throwable cause) {
        super(message, cause);
    }
}
