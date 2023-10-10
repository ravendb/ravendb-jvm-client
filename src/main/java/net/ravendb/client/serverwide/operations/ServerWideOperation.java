package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.KillOperationCommand;
import net.ravendb.client.documents.commands.KillServerOperationCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;

public class ServerWideOperation extends Operation {

    public ServerWideOperation(RequestExecutor requestExecutor, DocumentConventions conventions, long id) {
        this(requestExecutor, conventions, id, null);
    }

    public ServerWideOperation(RequestExecutor requestExecutor, DocumentConventions conventions, long id, String nodeTag) {
        super(requestExecutor, null, conventions, id, nodeTag);
        setNodeTag(nodeTag);
    }

    @Override
    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id) {
        return getOperationStateCommand(conventions, id, null);
    }

    @Override
    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id, String nodeTag) {
        return new GetServerWideOperationStateOperation.GetServerWideOperationStateCommand(id, nodeTag);
    }

    protected RavenCommand getKillOperationCommand(long id, String nodeTag) {
        return new KillServerOperationCommand(id, nodeTag);
    }
}
