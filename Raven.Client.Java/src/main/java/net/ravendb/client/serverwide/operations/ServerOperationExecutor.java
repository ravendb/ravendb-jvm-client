package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.http.ClusterRequestExecutor;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.VoidRavenCommand;

public class ServerOperationExecutor {

    private final ClusterRequestExecutor requestExecutor;

    public ServerOperationExecutor(DocumentStoreBase store) {
        requestExecutor = store.getConventions().isDisableTopologyUpdates() ?
                ClusterRequestExecutor.createForSingleNode(store.getUrls()[0]) :
                ClusterRequestExecutor.create(store.getUrls());
    }

    public void send(IVoidServerOperation operation) {
        VoidRavenCommand command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
    }

    public <TResult> TResult send(IServerOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);

        return command.getResult();
    }
}
