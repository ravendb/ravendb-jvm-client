package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.lang3.StringUtils;

public class OperationExecutor {

    private final IDocumentStore store;
    private final String databaseName;
    private final RequestExecutor requestExecutor;

    public OperationExecutor(DocumentStoreBase store) {
        this((IDocumentStore) store, null);
    }

    public OperationExecutor(DocumentStoreBase store, String databaseName) {
        this((IDocumentStore) store, databaseName);
    }

    protected OperationExecutor(IDocumentStore store, String databaseName) {
        this.store = store;
        this.databaseName = databaseName != null ? databaseName : store.getDatabase();
        if (StringUtils.isNotBlank(this.databaseName)) {
            this.requestExecutor = store.getRequestExecutor(this.databaseName);
        } else {
            throw new IllegalStateException("Cannot use operations without a database defined, did you forget to call forDatabase?");
        }
    }

    public OperationExecutor forDatabase(String databaseName) {
        if (StringUtils.equalsIgnoreCase(this.databaseName, databaseName)) {
            return this;
        }

        return new OperationExecutor(this.store, databaseName);
    }


    public <TResult> TResult send(IOperation<TResult> operation) {
        return send(operation, null);
    }

    public <TResult> TResult send(IOperation<TResult> operation, SessionInfo sessionInfo) {
        RavenCommand<TResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command, sessionInfo);

        return command.getResult();
    }



}
