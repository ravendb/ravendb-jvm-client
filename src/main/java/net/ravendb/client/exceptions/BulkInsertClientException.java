package net.ravendb.client.exceptions;

public class BulkInsertClientException extends RavenException {
    public BulkInsertClientException() {
    }

    public BulkInsertClientException(String message) {
        super(message);
    }

    public BulkInsertClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
