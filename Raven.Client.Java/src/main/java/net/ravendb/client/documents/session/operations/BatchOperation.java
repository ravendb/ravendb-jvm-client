package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.json.JsonArrayResult;

import java.util.List;

public class BatchOperation {

    private final InMemoryDocumentSessionOperations _session;

    public BatchOperation(InMemoryDocumentSessionOperations session) {
        this._session = session;
    }

    private List<Object> _entities;
    private int _sessionCommandsCount;

    public BatchCommand createRequest() {
        InMemoryDocumentSessionOperations.SaveChangesData result = _session.prepareForSaveChanges();

        _sessionCommandsCount = result.getSessionCommands().size();
        result.getSessionCommands().addAll(result.getDeferredCommands());
        if (result.getSessionCommands().isEmpty()) {
            return null;
        }

        _session.incrementRequestCount();

        _entities = result.getEntities();

        return new BatchCommand(_session.getConventions(), result.getSessionCommands(), result.getOptions());
    }

    public void setResult(JsonArrayResult result) {
        if (result.getResults() == null) {
            throwOnNullResults();
            return;
        }

        for (int i = 0; i < _sessionCommandsCount; i++) {
            ObjectNode batchResult = (ObjectNode) result.getResults().get(i);
            if (batchResult == null) {
                throw new IllegalArgumentException();
            }

            String type = batchResult.get("Type").asText();

            if (!"PUT".equals(type)) {
                continue;
            }

            Object entity = _entities.get(i);
            DocumentInfo documentInfo = _session.documentsByEntity.get(entity);
            if (documentInfo == null) {
                continue;
            }

            String changeVector = batchResult.get(Constants.Documents.Metadata.CHANGE_VECTOR).asText();
            if (changeVector == null) {
                throw new IllegalStateException("PUT response is invalid. @change-vector is missing on " + documentInfo.getId());
            }

            String id = batchResult.get(Constants.Documents.Metadata.ID).asText();
            if (id == null) {
                throw new IllegalStateException("PUT response is invalid. @id is missing on " + documentInfo.getId());
            }

            batchResult.fieldNames().forEachRemaining(propertyName -> {
                if ("Type".equals(propertyName)) {
                    return;
                }

                documentInfo.getMetadata().set(propertyName, batchResult.get(propertyName));
            });

            documentInfo.setId(id);
            documentInfo.setChangeVector(changeVector);
            documentInfo.getDocument().set(Constants.Documents.Metadata.KEY, documentInfo.getMetadata());
            //TODO: documentInfo.MetadataInstance = null;

            _session.documentsById.add(documentInfo);
            _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);

            /* TODO
             var afterStoreEventArgs = new AfterStoreEventArgs(_session, documentInfo.Id, documentInfo.Entity);
                _session.OnAfterStoreInvoke(afterStoreEventArgs);
             */
        }
    }

    private static void throwOnNullResults() {
        throw new IllegalStateException("Received empty response from the server. This is not supposed to happen and is likely a bug.");
    }

}
