package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.serverwide.operations.ServerOperationExecutor;
import org.apache.commons.lang3.StringUtils;

public class AdminOperationExecutor {

    private final DocumentStoreBase store;
    private final String databaseName;
    private RequestExecutor requestExecutor;
    private ServerOperationExecutor serverOperationExecutor;

    public AdminOperationExecutor(DocumentStoreBase store) {
        this(store, null);
    }

    public AdminOperationExecutor(DocumentStoreBase store, String databaseName) {
        this.store = store;
        this.databaseName = databaseName;
        this.requestExecutor = store.getRequestExecutor(databaseName);
    }

    public ServerOperationExecutor server() {
        if (serverOperationExecutor != null) {
            return serverOperationExecutor;
        } else {
            serverOperationExecutor = new ServerOperationExecutor(store);
            return serverOperationExecutor;
        }
    }

    public AdminOperationExecutor forDatabase(String databaseName) {
        if (StringUtils.equalsIgnoreCase(this.databaseName, databaseName)) {
            return this;
        }

        return new AdminOperationExecutor(store, databaseName);
    }

    public void send(IVoidAdminOperation operation) {
        RavenCommand command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
    }

    public <TResult> TResult send(IAdminOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
        return command.getResult();
    }

    /* TODO:
        public Operation Send(IAdminOperation<OperationIdResult> operation)
        {
            return AsyncHelpers.RunSync(() => SendAsync(operation));
        }

        public async Task<Operation> SendAsync(IAdminOperation<OperationIdResult> operation, CancellationToken token = default(CancellationToken))
        {
            JsonOperationContext context;
            using (RequestExecutor.ContextPool.AllocateOperationContext(out context))
            {
                var command = operation.GetCommand(_requestExecutor.Conventions, context);

                await _requestExecutor.ExecuteAsync(command, context, token).ConfigureAwait(false);
                return new Operation(_requestExecutor, () => _store.Changes(_databaseName), _requestExecutor.Conventions, command.Result.OperationId);
            }
        }
     */
}
