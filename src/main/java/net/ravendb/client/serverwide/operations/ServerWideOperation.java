package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;

public class ServerWideOperation extends Operation {

    public ServerWideOperation(RequestExecutor requestExecutor, DocumentConventions conventions, long id) {
        super(requestExecutor, null, conventions, id);
    }

    @Override
    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id) {
        return new GetServerWideOperationStateOperation.GetServerWideOperationStateCommand(conventions, id);
    }
}
