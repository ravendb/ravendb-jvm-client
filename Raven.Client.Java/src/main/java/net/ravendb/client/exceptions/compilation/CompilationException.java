package net.ravendb.client.exceptions.compilation;

import net.ravendb.client.exceptions.RavenException;

public class CompilationException extends RavenException {
    public CompilationException() {
    }

    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
