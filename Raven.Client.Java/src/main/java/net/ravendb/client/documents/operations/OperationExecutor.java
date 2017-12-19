package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

public class OperationExecutor {

    private final DocumentStoreBase store;
    private final String databaseName;
    private final RequestExecutor requestExecutor;

    public OperationExecutor(DocumentStoreBase store) {
        this(store, null);
    }

    public OperationExecutor(DocumentStoreBase store, String databaseName) {
        this.store = store;
        this.databaseName = databaseName != null ? databaseName : store.getDatabase();
        this.requestExecutor = store.getRequestExecutor(databaseName);
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

    public <TResult> void send(IOperation<TResult> operation) {
        send(operation, null);
    }

    public <TResult> TResult send(IOperation<TResult> operation, SessionInfo sessionInfo) {
        RavenCommand<TResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command);

        return command.getResult();
    }

    public Operation sendAsync(IOperation<OperationIdResult> operation) {
        return sendAsync(operation, null);
    }

    public Operation sendAsync(IOperation<OperationIdResult> operation, SessionInfo sessionInfo) {
        RavenCommand<OperationIdResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command, sessionInfo);

        return new Operation(requestExecutor, requestExecutor.getConventions(), command.getResult().getOperationId());
    }

    public PatchStatus send(PatchOperation operation) {
        RavenCommand<PatchResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command);

        if (command.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            return PatchStatus.NOT_MODIFIED;
        }

        if (command.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return PatchStatus.DOCUMENT_DOES_NOT_EXIST;
        }

        return command.getResult().getStatus();
    }

    @SuppressWarnings("unchecked")
    public <TEntity> PatchOperation.Result<TEntity> send(Class<TEntity> entityClass, PatchOperation operation) {
        RavenCommand<PatchResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command);

        PatchOperation.Result<TEntity> result = new PatchOperation.Result<>();

        if (command.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            result.setStatus(PatchStatus.NOT_MODIFIED);
            return result;
        }

        if (command.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            result.setStatus(PatchStatus.DOCUMENT_DOES_NOT_EXIST);
            return result;
        }

        result.setStatus(command.getResult().getStatus());
        result.setDocument((TEntity) requestExecutor.getConventions().deserializeEntityFromJson(entityClass, command.getResult().getModifiedDocument()));
        return result;
    }
}
