package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexCompactionInProgressException extends RavenException {
    public IndexCompactionInProgressException() {
    }

    public IndexCompactionInProgressException(String message) {
        super(message);
    }

    public IndexCompactionInProgressException(String message, Throwable cause) {
        super(message, cause);
    }
}
