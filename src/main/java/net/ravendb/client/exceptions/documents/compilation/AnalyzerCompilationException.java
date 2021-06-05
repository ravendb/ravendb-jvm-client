package net.ravendb.client.exceptions.documents.compilation;

import net.ravendb.client.exceptions.compilation.CompilationException;

public class AnalyzerCompilationException extends CompilationException {
    public AnalyzerCompilationException() {
    }

    public AnalyzerCompilationException(String message) {
        super(message);
    }

    public AnalyzerCompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
