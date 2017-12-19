package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexDeletionException extends RavenException {
    public IndexDeletionException() {
    }

    public IndexDeletionException(String message) {
        super(message);
    }

    public IndexDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
