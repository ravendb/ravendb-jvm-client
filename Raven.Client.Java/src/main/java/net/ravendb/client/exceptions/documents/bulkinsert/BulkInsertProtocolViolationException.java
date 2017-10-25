package net.ravendb.client.exceptions.documents.bulkinsert;

import net.ravendb.client.exceptions.RavenException;

public class BulkInsertProtocolViolationException extends RavenException {
    public BulkInsertProtocolViolationException() {
    }

    public BulkInsertProtocolViolationException(String message) {
        super(message);
    }

    public BulkInsertProtocolViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
