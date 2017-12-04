package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.serverwide.operations.ServerOperationExecutor;
import org.apache.commons.lang3.StringUtils;

public class MaintenanceOperationExecutor {

    private final DocumentStoreBase store;
    private final String databaseName;
    private RequestExecutor requestExecutor;
    private ServerOperationExecutor serverOperationExecutor;

    public MaintenanceOperationExecutor(DocumentStoreBase store) {
        this(store, null);
    }

    public MaintenanceOperationExecutor(DocumentStoreBase store, String databaseName) {
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

    public MaintenanceOperationExecutor forDatabase(String databaseName) {
        if (StringUtils.equalsIgnoreCase(this.databaseName, databaseName)) {
            return this;
        }

        return new MaintenanceOperationExecutor(store, databaseName);
    }

    public void send(IVoidMaintenanceOperation operation) {
        RavenCommand command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
    }

    public <TResult> TResult send(IMaintenanceOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
        return command.getResult();
    }

    public Operation sendOperation(IMaintenanceOperation<OperationIdResult> operation) {
        RavenCommand<OperationIdResult> command = operation.getCommand(requestExecutor.getConventions());

        requestExecutor.execute(command);
        return new Operation(requestExecutor, requestExecutor.getConventions(), command.getResult().getOperationId());
        //TBD pass changes as well
    }
}
