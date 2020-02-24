package net.ravendb.client.exceptions.compilation;

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
