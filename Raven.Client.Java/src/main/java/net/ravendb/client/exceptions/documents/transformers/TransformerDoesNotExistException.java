package net.ravendb.client.exceptions.documents.transformers;

import net.ravendb.client.exceptions.RavenException;

public class TransformerDoesNotExistException extends RavenException {
    public TransformerDoesNotExistException() {
    }

    public TransformerDoesNotExistException(String message) {
        super(message);
    }

    public TransformerDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
