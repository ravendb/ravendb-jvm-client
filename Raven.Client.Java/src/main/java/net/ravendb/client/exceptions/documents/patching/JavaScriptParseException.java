package net.ravendb.client.exceptions.documents.patching;

public class JavaScriptParseException extends JavaScriptException {
    public JavaScriptParseException() {
    }

    public JavaScriptParseException(String message) {
        super(message);
    }

    public JavaScriptParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
