package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexAlreadyExistException extends RavenException {
    public IndexAlreadyExistException() {
    }

    public IndexAlreadyExistException(String message) {
        super(message);
    }

    public IndexAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
