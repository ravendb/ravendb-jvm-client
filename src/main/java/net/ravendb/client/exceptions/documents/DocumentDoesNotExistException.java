package net.ravendb.client.exceptions.documents;

import net.ravendb.client.exceptions.RavenException;

public class DocumentDoesNotExistException extends RavenException {
    public DocumentDoesNotExistException() {
    }

    public DocumentDoesNotExistException(String id) {
        super("Document '" + id + "' does not exist.");
    }

    public DocumentDoesNotExistException(String id, Throwable cause) {
        super("Document '" + id + "' does not exist.", cause);
    }
}
