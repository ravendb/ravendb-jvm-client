package net.ravendb.client.exceptions;

public class BulkInsertInvalidOperationException extends BulkInsertClientException {
    public BulkInsertInvalidOperationException() {
    }

    public BulkInsertInvalidOperationException(String message) {
        super(message);
    }

    public BulkInsertInvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
