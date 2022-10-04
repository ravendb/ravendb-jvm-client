package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.changes.IObserver;
import net.ravendb.client.documents.changes.OperationStatusChange;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.RavenException;

public class BulkInsertObserver implements IObserver<OperationStatusChange> {
    private final BulkInsertOperation _operation;
    private final DocumentConventions _conventions;

    public BulkInsertObserver(BulkInsertOperation operation, DocumentConventions conventions) {
        _operation = operation;
        _conventions = conventions;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Exception error) {

    }

    @Override
    public void onNext(OperationStatusChange value) {
        if (value.getState() != null && "InProgress".equals(value.getState().get("Status").asText())) {
            JsonNode progressNode = value.getState().get("Progress");

            try {
                _operation.invokeOnProgress(_conventions.getEntityMapper().treeToValue(progressNode, BulkInsertProgress.class));
            } catch (JsonProcessingException e) {
                throw new RavenException("Unable to read bulk insert process", e);
            }
        }
    }
}
