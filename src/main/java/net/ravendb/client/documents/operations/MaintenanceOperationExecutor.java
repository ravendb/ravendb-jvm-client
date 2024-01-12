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
    private String _nodeTag;
    private Integer _shardNumber;
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

    public MaintenanceOperationExecutor(DocumentStore store, String databaseName, String nodeTag, Integer shardNumber) {
        this(store, databaseName);

        _nodeTag = nodeTag;
        _shardNumber = shardNumber;
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

        return new MaintenanceOperationExecutor(store, databaseName, _nodeTag, _shardNumber);
    }

    public void send(IVoidMaintenanceOperation operation) {
        assertDatabaseNameSet();
        VoidRavenCommand command = operation.getCommand(getRequestExecutor().getConventions());
        applyNodeTagAndShardNumberToCommandIfSet(command);
        getRequestExecutor().execute(command);
    }

    public <TResult> TResult send(IMaintenanceOperation<TResult> operation) {
        assertDatabaseNameSet();
        RavenCommand<TResult> command = operation.getCommand(getRequestExecutor().getConventions());
        applyNodeTagAndShardNumberToCommandIfSet(command);
        getRequestExecutor().execute(command);
        return command.getResult();
    }

    public Operation sendAsync(IMaintenanceOperation<OperationIdResult> operation) {
        assertDatabaseNameSet();
        RavenCommand<OperationIdResult> command = operation.getCommand(getRequestExecutor().getConventions());
        applyNodeTagAndShardNumberToCommandIfSet(command);
        getRequestExecutor().execute(command);
        String node = ObjectUtils.firstNonNull(command.getSelectedNodeTag(), command.getResult().getOperationNodeTag());
        return new Operation(getRequestExecutor(),
                () -> store.changes(databaseName, node), getRequestExecutor().getConventions(),
                command.getResult().getOperationId(),
                node);
    }

    private void assertDatabaseNameSet() {
        if (databaseName == null) {
            throw new IllegalStateException("Cannot use maintenance without a database defined, did you forget to call forDatabase?");
        }
    }

    private <T> void applyNodeTagAndShardNumberToCommandIfSet(RavenCommand<T> command) {
        if (_nodeTag != null) {
            command.setSelectedNodeTag(_nodeTag);
        }

        if (_shardNumber != null) {
            command.setSelectedShardNumber(_shardNumber);
        }
    }
}
