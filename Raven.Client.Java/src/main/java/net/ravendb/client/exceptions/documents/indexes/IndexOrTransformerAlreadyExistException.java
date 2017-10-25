package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexOrTransformerAlreadyExistException extends RavenException {
    public IndexOrTransformerAlreadyExistException() {
    }

    public IndexOrTransformerAlreadyExistException(String message) {
        super(message);
    }

    public IndexOrTransformerAlreadyExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
