package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.ClusterRequestExecutor;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.VoidRavenCommand;

public class ServerOperationExecutor {

    private final DocumentStoreBase store;
    private final ClusterRequestExecutor requestExecutor;

    public ServerOperationExecutor(DocumentStoreBase store) {
        this.store = store;
        requestExecutor = store.getConventions().isDisableTopologyUpdates() ?
                ClusterRequestExecutor.createForSingleNode(store.getUrls()[0], store.getCertificate()) :
                ClusterRequestExecutor.create(store.getUrls(), store.getCertificate());
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

    public Operation sendAsync(IServerOperation<OperationIdResult> operation) {
        RavenCommand<OperationIdResult> command = operation.getCommand(requestExecutor.getConventions());

        requestExecutor.execute(command);
        return new ServerWideOperation(requestExecutor, requestExecutor.getConventions(), command.getResult().getOperationId());
        //TBD pass changes as well
    }
}
