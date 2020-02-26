package net.ravendb.client.exceptions.documents.compilation;

import net.ravendb.client.exceptions.compilation.CompilationException;

public class SorterCompilationException extends CompilationException {
    public SorterCompilationException() {
    }

    public SorterCompilationException(String message) {
        super(message);
    }

    public SorterCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
