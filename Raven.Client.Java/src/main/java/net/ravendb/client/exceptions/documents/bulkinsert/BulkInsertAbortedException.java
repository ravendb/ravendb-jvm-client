package net.ravendb.client.exceptions.documents.bulkinsert;

import net.ravendb.client.exceptions.RavenException;

public class BulkInsertAbortedException extends RavenException {
    public BulkInsertAbortedException() {
    }

    public BulkInsertAbortedException(String message) {
        super(message);
    }

    public BulkInsertAbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
