package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.http.ClusterRequestExecutor;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.VoidRavenCommand;

public class ServerOperationExecutor {

    private final DocumentStoreBase store;
    private final ClusterRequestExecutor requestExecutor;

    public ServerOperationExecutor(DocumentStoreBase store) {
        this.store = store;
        requestExecutor = store.getConventions().isDisableTopologyUpdates() ?
                ClusterRequestExecutor.createForSingleNode(store.getUrls()[0]) :
                ClusterRequestExecutor.create(store.getUrls());
    }

    public void send(IVoidMaintenanceOperation operation) {
        VoidRavenCommand command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
    }

    public <TResult> TResult send(IMaintenanceOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);

        return command.getResult();
    }

    //TODO: public async Task<Operation> Send(IServerOperation<OperationIdResult> operation, CancellationToken token = default(CancellationToken))
}
