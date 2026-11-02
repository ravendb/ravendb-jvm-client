package net.ravendb.client.exceptions;

public final class QueryToolFailedException extends AiException {

    public QueryToolFailedException(String message) {
        super(message);
    }

    public QueryToolFailedException(String message, Exception e) {
        super(message, e);
    }
}
