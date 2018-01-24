package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.ExceptionDispatcher;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.OperationCancelledException;

public class Operation {

    private final RequestExecutor _requestExecutor;
    //TBD private readonly Func<IDatabaseChanges> _changes;
    private final DocumentConventions _conventions;
    private final long _id;

    //TBD public Action<IOperationProgress> OnProgressChanged;
    //TBD private IDisposable _subscription;

    public long getId() {
        return _id;
    }

    public Operation(RequestExecutor requestExecutor, DocumentConventions conventions, long id) {
        _requestExecutor = requestExecutor;
        //TBD _changes = changes;
        _conventions = conventions;
        _id = id;
    }

    //TBD currently we simply pull for status - implement this using changes API

    private ObjectNode fetchOperationsStatus() {
        RavenCommand<ObjectNode> command = getOperationStateCommand(_conventions, _id);
        _requestExecutor.execute(command);

        return command.getResult();
    }

    protected RavenCommand<ObjectNode> getOperationStateCommand(DocumentConventions conventions, long id) {
        return new GetOperationStateOperation.GetOperationStateCommand(_conventions, _id);
    }
    public void waitForCompletion() {
        while (true) {
            ObjectNode status = fetchOperationsStatus();

            String operationStatus = status.get("Status").asText();
            switch (operationStatus) {
                case "Completed":
                    return ;
                case "Cancelled":
                    throw new OperationCancelledException();
                case "Faulted":
                    JsonNode result = status.get("Result");
                    OperationExceptionResult exceptionResult = JsonExtensions.getDefaultMapper().convertValue(result, OperationExceptionResult.class);
                    throw ExceptionDispatcher.get(exceptionResult.getMessage(), exceptionResult.getError(), exceptionResult.getType(), exceptionResult.getStatusCode());
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
