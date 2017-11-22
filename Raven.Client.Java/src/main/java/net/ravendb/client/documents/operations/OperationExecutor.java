package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.lang3.StringUtils;

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
        send(operation, null, false);
    }

    public void send(IVoidOperation operation, SessionInfo sessionInfo) {
        send(operation, sessionInfo, false);
    }

    public void send(IVoidOperation operation, SessionInfo sessionInfo, boolean isServerOperation) {
        RavenCommand<Void> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command, sessionInfo);
    }

    public <TResult> void send(IOperation<TResult> operation) {
        send(operation, null, false);
    }

    public <TResult> void send(IOperation<TResult> operation, SessionInfo sessionInfo) {
        send(operation, sessionInfo, false);
    }

    public <TResult> TResult send(IOperation<TResult> operation, SessionInfo sessionInfo, boolean isServerOperation) {
        RavenCommand<TResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());
        requestExecutor.execute(command);

        return command.getResult();
    }

    public Operation sendOperation(IOperation<OperationIdResult> operation) {
        return sendOperation(operation, null, false);
    }

    public Operation sendOperation(IOperation<OperationIdResult> operation, SessionInfo sessionInfo) {
        return sendOperation(operation, sessionInfo, false);
    }

    public Operation sendOperation(IOperation<OperationIdResult> operation, SessionInfo sessionInfo, boolean isServerOperation) {
        RavenCommand<OperationIdResult> command = operation.getCommand(store, requestExecutor.getConventions(), requestExecutor.getCache());

        requestExecutor.execute(command, sessionInfo);

        return new Operation(requestExecutor, requestExecutor.getConventions(), command.getResult().getOperationId(), isServerOperation);
    }


    /* TODO

           public PatchStatus Send(PatchOperation operation)
        {
            return AsyncHelpers.RunSync(() => SendAsync(operation));
        }

        public async Task<PatchStatus> SendAsync(PatchOperation operation, CancellationToken token = default(CancellationToken))
        {
            JsonOperationContext context;
            using (GetContext(out context))
            {
                var command = operation.GetCommand(_store, _requestExecutor.Conventions, context, _requestExecutor.Cache);

                await _requestExecutor.ExecuteAsync(command, context, token).ConfigureAwait(false);

                if (command.StatusCode == HttpStatusCode.NotModified)
                    return PatchStatus.NotModified;

                if (command.StatusCode == HttpStatusCode.NotFound)
                    return PatchStatus.DocumentDoesNotExist;

                return command.Result.Status;
            }
        }

        public PatchOperation.Result<TEntity> Send<TEntity>(PatchOperation<TEntity> operation)
        {
            return AsyncHelpers.RunSync(() => SendAsync(operation));
        }

        public async Task<PatchOperation.Result<TEntity>> SendAsync<TEntity>(PatchOperation<TEntity> operation, CancellationToken token = default(CancellationToken))
        {
            JsonOperationContext context;
            using (GetContext(out context))
            {
                var command = operation.GetCommand(_store, _requestExecutor.Conventions, context, _requestExecutor.Cache);

                await _requestExecutor.ExecuteAsync(command, context, token).ConfigureAwait(false);

                var result = new PatchOperation.Result<TEntity>();

                if (command.StatusCode == HttpStatusCode.NotModified)
                {
                    result.Status = PatchStatus.NotModified;
                    return result;
                }

                if (command.StatusCode == HttpStatusCode.NotFound)
                {
                    result.Status = PatchStatus.DocumentDoesNotExist;
                    return result;
                }

                result.Status = command.Result.Status;
                result.Document = (TEntity)_requestExecutor.Conventions.DeserializeEntityFromBlittable(typeof(TEntity), command.Result.ModifiedDocument);
                return result;
            }
        }
     */
}
