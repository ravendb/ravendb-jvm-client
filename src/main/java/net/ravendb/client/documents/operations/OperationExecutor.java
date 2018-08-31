package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.io.IOException;

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
        if (this.databaseName != null) {
            this.requestExecutor = store.getRequestExecutor(databaseName);
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

    public void send(IVoidOperation operation) {
        send(operation, null);
    }

    public void send(IVoidOperation operation, SessionInfo sessionInfo) {
        RavenCommand<Void> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command, sessionInfo);
    }

    public <TResult> TResult send(IOperation<TResult> operation) {
        return send(operation, null);
    }

    public <TResult> TResult send(IOperation<TResult> operation, SessionInfo sessionInfo) {
        RavenCommand<TResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command, sessionInfo);

        return command.getResult();
    }

    public Operation sendAsync(IOperation<OperationIdResult> operation) {
        return sendAsync(operation, null);
    }

    public Operation sendAsync(IOperation<OperationIdResult> operation, SessionInfo sessionInfo) {
        RavenCommand<OperationIdResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command, sessionInfo);

        return new Operation(requestExecutor, () -> store.changes(), requestExecutor.getConventions(), command.getResult().getOperationId());
    }

    public PatchStatus send(PatchOperation operation) {
        return send(operation, null);
    }

    public PatchStatus send(PatchOperation operation, SessionInfo sessionInfo) {
        RavenCommand<PatchResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command, sessionInfo);

        if (command.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            return PatchStatus.NOT_MODIFIED;
        }

        if (command.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return PatchStatus.DOCUMENT_DOES_NOT_EXIST;
        }

        return command.getResult().getStatus();
    }

    public <TEntity> PatchOperation.Result<TEntity> send(Class<TEntity> entityClass, PatchOperation operation) {
        return send(entityClass, operation, null);
    }

    @SuppressWarnings("unchecked")
    public <TEntity> PatchOperation.Result<TEntity> send(Class<TEntity> entityClass, PatchOperation operation, SessionInfo sessionInfo) {
        RavenCommand<PatchResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command, sessionInfo);

        PatchOperation.Result<TEntity> result = new PatchOperation.Result<>();

        if (command.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            result.setStatus(PatchStatus.NOT_MODIFIED);
            return result;
        }

        if (command.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            result.setStatus(PatchStatus.DOCUMENT_DOES_NOT_EXIST);
            return result;
        }

        try {
            result.setStatus(command.getResult().getStatus());
            result.setDocument(requestExecutor.getConventions().getEntityMapper().treeToValue(command.getResult().getModifiedDocument(), entityClass));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read patch result: " + e.getMessage(), e);
        }
    }
}
