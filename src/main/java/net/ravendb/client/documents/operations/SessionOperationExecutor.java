package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

/**
 * For internal session use only
 */
public class SessionOperationExecutor extends OperationExecutor {

    private final InMemoryDocumentSessionOperations _session;

    public SessionOperationExecutor(InMemoryDocumentSessionOperations session) {
        super(session.getDocumentStore(), session.getDatabaseName());

        _session = session;
    }

    @Override
    public OperationExecutor forDatabase(String databaseName) {
        throw new IllegalStateException("The method is not supported");
    }

}
