package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.SingleNodeBatchCommand;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.json.BatchCommandResult;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class BatchOperation {

    private final InMemoryDocumentSessionOperations _session;

    public BatchOperation(InMemoryDocumentSessionOperations session) {
        this._session = session;
    }

    private List<Object> _entities;
    private int _sessionCommandsCount;
    private int _allCommandsCount;
    private InMemoryDocumentSessionOperations.SaveChangesData.ActionsToRunOnSuccess _onSuccessfulRequest;

    private Map<String, DocumentInfo> _modifications;

    public SingleNodeBatchCommand createRequest() {
        InMemoryDocumentSessionOperations.SaveChangesData result = _session.prepareForSaveChanges();
        _onSuccessfulRequest = result.getOnSuccess();
        _sessionCommandsCount = result.getSessionCommands().size();
        result.getSessionCommands().addAll(result.getDeferredCommands());

        _allCommandsCount = result.getSessionCommands().size();

        if (_allCommandsCount == 0) {
            return null;
        }

        _session.incrementRequestCount();

        _entities = result.getEntities();


        return new SingleNodeBatchCommand(_session.getConventions(), result.getSessionCommands());
    }

    public void setResult(BatchCommandResult result) {

        Function<ObjectNode, CommandType> getCommandType = batchResult -> {
            JsonNode type = batchResult.get("Type");

            if (type == null || !type.isTextual()) {
                return CommandType.NONE;
            }

            String typeAsString = type.asText();

            CommandType commandType = CommandType.parseCSharpValue(typeAsString);
            return commandType;
        };

        if (result.getResults() == null) {
            throwOnNullResults();
            return;
        }

        _onSuccessfulRequest.clearSessionStateAfterSuccessfulSaveChanges();



        for (int i = 0; i < _sessionCommandsCount; i++) {
            ObjectNode batchResult = (ObjectNode) result.getResults().get(i);
            if (batchResult == null) {
                continue;
            }

            CommandType type = getCommandType.apply(batchResult);

            switch (type) {
                case PUT:
                    handlePut(i, batchResult, false);
                    break;
                case DELETE:
                    handleDelete(batchResult);
                    break;
                default:
                    throw new IllegalStateException("Command " + type + " is not supported");
            }
        }

        for (int i = _sessionCommandsCount; i < _allCommandsCount; i++) {
            ObjectNode batchResult = (ObjectNode) result.getResults().get(i);
            if (batchResult == null) {
                continue;
            }

            CommandType type = getCommandType.apply(batchResult);

            switch (type) {
                case PUT:
                    handlePut(i, batchResult, true);
                    break;
                case DELETE:
                    handleDelete(batchResult);
                    break;
                default:
                    throw new IllegalStateException("Command " + type + " is not supported");
            }
        }
        finalizeResult();
    }

    private void finalizeResult() {
        if (_modifications == null || _modifications.isEmpty()) {
            return;
        }

        for (Map.Entry<String, DocumentInfo> kvp : _modifications.entrySet()) {
            String id = kvp.getKey();
            DocumentInfo documentInfo = kvp.getValue();

            applyMetadataModifications(id, documentInfo);
        }
    }

    private void applyMetadataModifications(String id, DocumentInfo documentInfo) {
        documentInfo.setMetadataInstance(null);

        documentInfo.setMetadata(documentInfo.getMetadata().deepCopy());

        documentInfo.getMetadata().set(Constants.Documents.Metadata.CHANGE_VECTOR,
                documentInfo.getMetadata().textNode(documentInfo.getChangeVector()));

        ObjectNode documentCopy = documentInfo.getDocument().deepCopy();
        documentCopy.set(Constants.Documents.Metadata.KEY, documentInfo.getMetadata());

        documentInfo.setDocument(documentCopy);
    }

    private DocumentInfo getOrAddModifications(String id, DocumentInfo documentInfo, boolean applyModifications) {
        if (_modifications == null) {
            _modifications = new TreeMap<>(String::compareToIgnoreCase);
        }

        DocumentInfo modifiedDocumentInfo = _modifications.get(id);
        if (modifiedDocumentInfo != null) {
            if (applyModifications) {
                applyMetadataModifications(id, modifiedDocumentInfo);
            }
        } else {
            _modifications.put(id, modifiedDocumentInfo = documentInfo);
        }

        return modifiedDocumentInfo;
    }


    private void handleDelete(ObjectNode batchReslt) {
        handleDeleteInternal(batchReslt, CommandType.DELETE);
    }

    private void handleDeleteInternal(ObjectNode batchResult, CommandType type) {
        String id = getStringField(batchResult, type, "Id");

        DocumentInfo documentInfo = _session.documentsById.getValue(id);
        if (documentInfo == null) {
            return;
        }

        _session.documentsById.remove(id);

        if (documentInfo.getEntity() != null) {
            _session.documentsByEntity.remove(documentInfo.getEntity());
            _session.deletedEntities.remove(documentInfo.getEntity());
        }
    }

    private void handlePut(int index, ObjectNode batchResult, boolean isDeferred) {
        Object entity = null;
        DocumentInfo documentInfo = null;

        if (!isDeferred) {
            entity = _entities.get(index);

            documentInfo = _session.documentsByEntity.get(entity);
            if (documentInfo == null) {
                return;
            }
        }

        String id = getStringField(batchResult, CommandType.PUT, Constants.Documents.Metadata.ID);
        String changeVector = getStringField(batchResult, CommandType.PUT, Constants.Documents.Metadata.CHANGE_VECTOR);

        if (isDeferred) {
            DocumentInfo sessionDocumentInfo = _session.documentsById.getValue(id);
            if (sessionDocumentInfo == null) {
                return;
            }

            documentInfo = getOrAddModifications(id, sessionDocumentInfo, true);
            entity = documentInfo.getEntity();
        }

        handleMetadataModifications(documentInfo, batchResult, id, changeVector);

        _session.documentsById.add(documentInfo);

        if (entity != null) {
            _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);
        }

        AfterSaveChangesEventArgs afterSaveChangesEventArgs = new AfterSaveChangesEventArgs(_session, documentInfo.getId(), documentInfo.getEntity());
        _session.onAfterSaveChangesInvoke(afterSaveChangesEventArgs);
    }

    private void handleMetadataModifications(DocumentInfo documentInfo, ObjectNode batchResult, String id, String changeVector) {
        Iterator<String> fieldsIterator = batchResult.fieldNames();

        while (fieldsIterator.hasNext()) {
            String propertyName = fieldsIterator.next();

            if ("Type".equals(propertyName)) {
                continue;
            }

            documentInfo.getMetadata().set(propertyName, batchResult.get(propertyName));
        }

        documentInfo.setId(id);
        documentInfo.setChangeVector(changeVector);

        applyMetadataModifications(id, documentInfo);
    }



    private static String getStringField(ObjectNode json, CommandType type, String fieldName) {
        return getStringField(json, type, fieldName, true);
    }

    private static String getStringField(ObjectNode json, CommandType type, String fieldName, boolean throwOnMissing) {
        JsonNode jsonNode = json.get(fieldName);
        if ((jsonNode == null || jsonNode.isNull()) && throwOnMissing) {
            throwMissingField(type, fieldName);
        }

        return jsonNode.asText();
    }

    private static Long getLongField(ObjectNode json, CommandType type, String fieldName) {
        JsonNode jsonNode = json.get(fieldName);
        if (jsonNode == null || !jsonNode.isNumber()) {
            throwMissingField(type, fieldName);
        }

        return jsonNode.asLong();
    }

    private static boolean getBooleanField(ObjectNode json, CommandType type, String fieldName) {
        JsonNode jsonNode = json.get(fieldName);
        if (jsonNode == null || !jsonNode.isBoolean()) {
            throwMissingField(type, fieldName);
        }

        return jsonNode.asBoolean();
    }


    private static void throwMissingField(CommandType type, String fieldName) {
        throw new IllegalStateException(type + " response is invalid. Field '" + fieldName + "' is missing.");
    }

    private static void throwOnNullResults() {
        throw new IllegalStateException("Received empty response from the server. This is not supposed to happen and is likely a bug.");
    }

}
