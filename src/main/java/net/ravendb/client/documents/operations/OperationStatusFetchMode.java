package net.ravendb.client.documents.operations;

public enum OperationStatusFetchMode {
    /**
     * Uses the Changes API to fetch the status
     */
    CHANGES_API,
    /**
     * Uses simple HTTP polling to fetch the status. Suitable for systems that do not support Changes API capabilities like WebSockets
     */
    POLLING
}
