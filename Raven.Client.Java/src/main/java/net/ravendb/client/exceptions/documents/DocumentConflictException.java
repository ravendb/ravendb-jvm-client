package net.ravendb.client.exceptions.documents;

import net.ravendb.client.exceptions.ConflictException;

public class DocumentConflictException extends ConflictException {
    public DocumentConflictException() {
    }

    public DocumentConflictException(String message) {
        super(message);
    }

    public DocumentConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
