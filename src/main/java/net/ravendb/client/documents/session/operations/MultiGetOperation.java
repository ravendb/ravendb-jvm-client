package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.MultiGetCommand;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.util.List;

public class MultiGetOperation {
    private final InMemoryDocumentSessionOperations _session;

    public MultiGetOperation(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public MultiGetCommand createRequest(List<GetRequest> requests) {
        return new MultiGetCommand(_session.getRequestExecutor().getCache(), requests);
    }

    @SuppressWarnings("EmptyMethod")
    public void setResult(ObjectNode result) {

    }
}
