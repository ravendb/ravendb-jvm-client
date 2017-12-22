package net.ravendb.client.exceptions.documents.indexes;

import net.ravendb.client.exceptions.RavenException;

public class IndexCreationException extends RavenException {

    public IndexCreationException() {
    }

    public IndexCreationException(String message) {
        super(message);
    }

    public IndexCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
