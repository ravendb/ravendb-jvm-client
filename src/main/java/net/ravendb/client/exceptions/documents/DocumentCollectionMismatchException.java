package net.ravendb.client.exceptions.documents;

import net.ravendb.client.exceptions.RavenException;

/**
 * This exception is raised when stored document has a collection mismatch
 */
public class DocumentCollectionMismatchException extends RavenException {
    public DocumentCollectionMismatchException() {
    }

    public DocumentCollectionMismatchException(String message) {
        super(message);
    }

    public DocumentCollectionMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
