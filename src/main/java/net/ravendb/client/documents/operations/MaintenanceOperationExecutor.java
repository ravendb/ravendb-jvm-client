package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.serverwide.operations.ServerOperationExecutor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class MaintenanceOperationExecutor {

    private final DocumentStore store;
    private final String databaseName;
    private RequestExecutor requestExecutor;
    private ServerOperationExecutor serverOperationExecutor;

    public MaintenanceOperationExecutor(DocumentStore store) {
        this(store, null);
    }

    public MaintenanceOperationExecutor(DocumentStore store, String databaseName) {
        this.store = store;
        this.databaseName = ObjectUtils.firstNonNull(databaseName, store.getDatabase());
    }

    private RequestExecutor getRequestExecutor() {
        if (requestExecutor != null) {
            return requestExecutor;
        }

        requestExecutor = this.databaseName != null ? store.getRequestExecutor(this.databaseName) : null;
        return requestExecutor;
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
        assertDatabaseNameSet();
        VoidRavenCommand command = operation.getCommand(getRequestExecutor().getConventions());
        getRequestExecutor().execute(command);
    }

    public <TResult> TResult send(IMaintenanceOperation<TResult> operation) {
        assertDatabaseNameSet();
        RavenCommand<TResult> command = operation.getCommand(getRequestExecutor().getConventions());
        getRequestExecutor().execute(command);
        return command.getResult();
    }

    public Operation sendAsync(IMaintenanceOperation<OperationIdResult> operation) {
        assertDatabaseNameSet();
        RavenCommand<OperationIdResult> command = operation.getCommand(getRequestExecutor().getConventions());

        getRequestExecutor().execute(command);
        return new Operation(getRequestExecutor(), () -> store.changes(), getRequestExecutor().getConventions(), command.getResult().getOperationId());
    }

    private void assertDatabaseNameSet() {
        if (databaseName == null) {
            throw new IllegalStateException("Cannot use maintenance without a database defined, did you forget to call forDatabase?");
        }
    }
}
