package net.ravendb.client.exceptions.documents.patching;

import net.ravendb.client.exceptions.RavenException;

public class JavaScriptException extends RavenException {
    public JavaScriptException() {
    }

    public JavaScriptException(String message) {
        super(message);
    }

    public JavaScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
