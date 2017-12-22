package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexDoesNotExistException extends RavenException {
    public IndexDoesNotExistException() {
    }

    public IndexDoesNotExistException(String message) {
        super(message);
    }

    public IndexDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
