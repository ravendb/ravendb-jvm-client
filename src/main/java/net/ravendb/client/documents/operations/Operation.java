package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.ExceptionDispatcher;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.OperationCancelledException;

import java.util.function.Supplier;

public class Operation {

    private final RequestExecutor _requestExecutor;
    private final DocumentConventions _conventions;
    private final long _id;
    private String _nodeTag;

    public long getId() {
        return _id;
    }

    public Operation(RequestExecutor requestExecutor, Supplier<IDatabaseChanges> changes, DocumentConventions conventions, long id) {
        this(requestExecutor, changes, conventions, id, null);
    }

    public Operation(RequestExecutor requestExecutor, Supplier<IDatabaseChanges> changes, DocumentConventions conventions, long id, String nodeTag) {
        _requestExecutor = requestExecutor;
        _conventions = conventions;
        _id = id;
        _nodeTag = nodeTag;
    }

    private ObjectNode fetchOperationsStatus() {
        RavenCommand<ObjectNode> command = getOperationStateCommand(_conventions, _id, _nodeTag);
        _requestExecutor.execute(command);

        return command.getResult();
    }

    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id) {
        return getOperationStateCommand(conventions, id, null);
    }

    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id, String nodeTag) {
        return new GetOperationStateOperation.GetOperationStateCommand(_conventions, _id, nodeTag);
    }

    public String getNodeTag() {
        return _nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        _nodeTag = nodeTag;
    }

    public void waitForCompletion() {
        while (true) {
            ObjectNode status = fetchOperationsStatus();

            String operationStatus = status.get("Status").asText();
            switch (operationStatus) {
                case "Completed":
                    return ;
                case "Canceled":
                    throw new OperationCancelledException();
                case "Faulted":
                    JsonNode result = status.get("Result");
                    OperationExceptionResult exceptionResult = JsonExtensions.getDefaultMapper().convertValue(result, OperationExceptionResult.class);
                    ExceptionDispatcher.ExceptionSchema schema = new ExceptionDispatcher.ExceptionSchema();
                    schema.setUrl(_requestExecutor.getUrl());
                    schema.setError(exceptionResult.getError());
                    schema.setMessage(exceptionResult.getMessage());
                    schema.setType(exceptionResult.getType());
                    throw ExceptionDispatcher.get(schema, exceptionResult.getStatusCode());
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
