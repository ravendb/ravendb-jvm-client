package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public class GetServerWideOperationStateOperation implements IServerOperation<ObjectNode> {

    private final long _id;

    public GetServerWideOperationStateOperation(long id) {
        _id = id;
    }

    @Override
    public RavenCommand<ObjectNode> getCommand(DocumentConventions conventions) {
        return new GetServerWideOperationStateCommand(DocumentConventions.defaultConventions, _id);
    }
}
