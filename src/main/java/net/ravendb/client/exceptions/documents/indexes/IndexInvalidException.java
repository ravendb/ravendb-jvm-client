package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexInvalidException extends RavenException {
    public IndexInvalidException() {
    }

    public IndexInvalidException(String message) {
        super(message);
    }

    public IndexInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
