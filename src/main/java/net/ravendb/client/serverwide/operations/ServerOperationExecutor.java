package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.ClusterRequestExecutor;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.CleanCloseable;

public class ServerOperationExecutor implements CleanCloseable {

    private final ClusterRequestExecutor requestExecutor;

    public ServerOperationExecutor(DocumentStore store) {
        requestExecutor = store.getConventions().isDisableTopologyUpdates() ?
                ClusterRequestExecutor.createForSingleNode(store.getUrls()[0], store.getCertificate(), store.getTrustStore(), store.getExecutorService(), store.getConventions()) :
                ClusterRequestExecutor.create(store.getUrls(), store.getCertificate(), store.getTrustStore(), store.getExecutorService(), store.getConventions());

        store.addAfterCloseListener((sender, event) -> requestExecutor.close());
    }

    public void send(IVoidServerOperation operation) {
        VoidRavenCommand command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);
    }

    @SuppressWarnings("UnusedReturnValue")
    public <TResult> TResult send(IServerOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(requestExecutor.getConventions());
        requestExecutor.execute(command);

        return command.getResult();
    }

    public Operation sendAsync(IServerOperation<OperationIdResult> operation) {
        RavenCommand<OperationIdResult> command = operation.getCommand(requestExecutor.getConventions());

        requestExecutor.execute(command);
        return new ServerWideOperation(requestExecutor, requestExecutor.getConventions(), command.getResult().getOperationId());
    }

    @Override
    public void close() {
        requestExecutor.close();
    }
}
