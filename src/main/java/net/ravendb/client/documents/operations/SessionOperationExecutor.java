package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

/**
 * For internal session use only
 */
public class SessionOperationExecutor extends OperationExecutor {

    private final InMemoryDocumentSessionOperations _session;

    /**
     * This constructor should not be used
     * @param store DocumentStore
     */
    @Deprecated
    public SessionOperationExecutor(DocumentStoreBase store) {
        super(store);
        _session = null;
    }

    public SessionOperationExecutor(DocumentStoreBase store, String databaseName) {
        super(store, databaseName);
        _session = null;
    }

    public SessionOperationExecutor(InMemoryDocumentSessionOperations session) {
        super(session.getDocumentStore(), session.getDatabaseName());

        _session = session;
    }

    @Override
    public OperationExecutor forDatabase(String databaseName) {
        throw new IllegalStateException("The method is not supported");
    }

}
