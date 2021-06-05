package net.ravendb.client.exceptions.documents.analyzers;

import net.ravendb.client.exceptions.RavenException;

public class AnalyzerDoesNotExistException extends RavenException {
    public AnalyzerDoesNotExistException() {
    }

    public AnalyzerDoesNotExistException(String message) {
        super(message);
    }

    public AnalyzerDoesNotExistException(String message, Throwable cause) {
        super(message, cause);
    }
}
