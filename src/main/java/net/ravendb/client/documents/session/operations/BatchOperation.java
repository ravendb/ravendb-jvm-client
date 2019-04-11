package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.operations.PatchStatus;
import net.ravendb.client.documents.session.AfterSaveChangesEventArgs;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.ClientVersionMismatchException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.MetadataAsDictionary;
import net.ravendb.client.primitives.Tuple;

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

    public BatchCommand createRequest() {
        InMemoryDocumentSessionOperations.SaveChangesData result = _session.prepareForSaveChanges();
        _onSuccessfulRequest = result.getOnSuccess();
        _sessionCommandsCount = result.getSessionCommands().size();
        result.getSessionCommands().addAll(result.getDeferredCommands());

        _session.validateClusterTransaction(result);

        _allCommandsCount = result.getSessionCommands().size();

        if (_allCommandsCount == 0) {
            return null;
        }

        _session.incrementRequestCount();

        _entities = result.getEntities();

        return new BatchCommand(_session.getConventions(), result.getSessionCommands(), result.getOptions(), _session.getTransactionMode());
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

        if (_session.getTransactionMode() == TransactionMode.CLUSTER_WIDE) {
            if (result.getTransactionIndex() <= 0) {
                throw new ClientVersionMismatchException("Cluster transaction was send to a node that is not supporting it. " +
                        "So it was executed ONLY on the requested node on " + _session.getRequestExecutor().getUrl());
            }
        }

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
                case COMPARE_EXCHANGE_PUT:
                case COMPARE_EXCHANGE_DELETE:
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
                case PATCH:
                    handlePatch(batchResult);
                    break;
                case ATTACHMENT_PUT:
                    handleAttachmentPut(batchResult);
                    break;
                case ATTACHMENT_DELETE:
                    handleAttachmentDelete(batchResult);
                    break;
                case ATTACHMENT_MOVE:
                    handleAttachmentMove(batchResult);
                    break;
                case ATTACHMENT_COPY:
                    handleAttachmentCopy(batchResult);
                    break;
                case COMPARE_EXCHANGE_PUT:
                case COMPARE_EXCHANGE_DELETE:
                    break;
                case COUNTERS:
                    handleCounters(batchResult);
                    break;
                case BATCH_PATCH:
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
        MetadataAsDictionary metadata = new MetadataAsDictionary(documentInfo.getMetadata());
        documentInfo.setMetadataInstance(metadata);
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

    private void handleAttachmentCopy(ObjectNode batchResult) {
        handleAttachmentPutInternal(batchResult, CommandType.ATTACHMENT_COPY, "Id", "Name");
    }

    private void handleAttachmentMove(ObjectNode batchResult) {
        handleAttachmentDeleteInternal(batchResult, CommandType.ATTACHMENT_MOVE, "Id", "Name");
        handleAttachmentPutInternal(batchResult, CommandType.ATTACHMENT_MOVE, "DestinationId", "DestinationName");
    }

    private void handleAttachmentDelete(ObjectNode batchResult) {
        handleAttachmentDeleteInternal(batchResult, CommandType.ATTACHMENT_DELETE, Constants.Documents.Metadata.ID, "Name");
    }

    private void handleAttachmentDeleteInternal(ObjectNode batchResult, CommandType type, String idFieldName, String attachmentNameFieldName) {
        String id = getStringField(batchResult, type, idFieldName);

        DocumentInfo sessionDocumentInfo = _session.documentsById.getValue(id);
        if (sessionDocumentInfo == null) {
            return;
        }

        DocumentInfo documentInfo = getOrAddModifications(id, sessionDocumentInfo, true);

        JsonNode attachmentsJson = documentInfo.getMetadata().get(Constants.Documents.Metadata.ATTACHMENTS);
        if (attachmentsJson == null || attachmentsJson.isNull() || attachmentsJson.size() == 0) {
            return;
        }

        String name = getStringField(batchResult, type, attachmentNameFieldName);

        ArrayNode attachments = JsonExtensions.getDefaultMapper().createArrayNode();
        documentInfo.getMetadata().set(Constants.Documents.Metadata.ATTACHMENTS, attachments);

        for (int i = 0; i < attachmentsJson.size(); i++) {
            ObjectNode attachment = (ObjectNode) attachmentsJson.get(i);
            String attachmentName = getStringField(attachment, type, "Name");
            if (attachmentName.equals(name)) {
                continue;
            }

            attachments.add(attachment);
        }
    }

    private void handleAttachmentPut(ObjectNode batchResult) {
        handleAttachmentPutInternal(batchResult, CommandType.ATTACHMENT_PUT, "Id", "Name");
    }

    private void handleAttachmentPutInternal(ObjectNode batchResult, CommandType type, String idFieldName, String attachmentNameFieldName) {
        String id = getStringField(batchResult, type, idFieldName);

        DocumentInfo sessionDocumentInfo = _session.documentsById.getValue(id);
        if (sessionDocumentInfo == null) {
            return;
        }

        DocumentInfo documentInfo = getOrAddModifications(id, sessionDocumentInfo, false);

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        ArrayNode attachments = (ArrayNode) documentInfo.getMetadata().get(Constants.Documents.Metadata.ATTACHMENTS);
        if (attachments == null) {
            attachments = mapper.createArrayNode();
            documentInfo.getMetadata().set(Constants.Documents.Metadata.ATTACHMENTS, attachments);
        }

        ObjectNode dynamicNode = mapper.createObjectNode();
        attachments.add(dynamicNode);
        dynamicNode.put("ChangeVector", getStringField(batchResult, type, "ChangeVector"));
        dynamicNode.put("ContentType", getStringField(batchResult, type, "ContentType"));
        dynamicNode.put("Hash", getStringField(batchResult, type, "Hash"));
        dynamicNode.put("Name", getStringField(batchResult, type, "Name"));
        dynamicNode.put("Size", getLongField(batchResult, type, "Size"));
    }

    private void handlePatch(ObjectNode batchResult) {

        JsonNode patchStatus = batchResult.get("PatchStatus");
        if (patchStatus == null || patchStatus.isNull()) {
            throwMissingField(CommandType.PATCH, "PatchStatus");
        }

        PatchStatus status = JsonExtensions.getDefaultMapper().convertValue(patchStatus, PatchStatus.class);

        switch (status) {
            case CREATED:
            case PATCHED:
                ObjectNode document = (ObjectNode) batchResult.get("ModifiedDocument");
                if (document == null) {
                    return;
                }

                String id = getStringField(batchResult, CommandType.PUT, "Id");

                DocumentInfo sessionDocumentInfo = _session.documentsById.getValue(id);
                if (sessionDocumentInfo == null) {
                    return;
                }

                DocumentInfo documentInfo = getOrAddModifications(id, sessionDocumentInfo, true);

                String changeVector = getStringField(batchResult, CommandType.PATCH, "ChangeVector");
                String lastModified = getStringField(batchResult, CommandType.PATCH, "LastModified");

                documentInfo.setChangeVector(changeVector);

                documentInfo.getMetadata().put(Constants.Documents.Metadata.ID, id);
                documentInfo.getMetadata().put(Constants.Documents.Metadata.CHANGE_VECTOR, changeVector);
                documentInfo.getMetadata().put(Constants.Documents.Metadata.LAST_MODIFIED, lastModified);

                documentInfo.setDocument(document);
                applyMetadataModifications(id, documentInfo);

                if (documentInfo.getEntity() != null) {
                    _session.getEntityToJson().populateEntity(documentInfo.getEntity(), id, documentInfo.getDocument());
                }

                break;
        }
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

        _session.documentsById.add(documentInfo);

        if (entity != null) {
            _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);
        }

        AfterSaveChangesEventArgs afterSaveChangesEventArgs = new AfterSaveChangesEventArgs(_session, documentInfo.getId(), documentInfo.getEntity());
        _session.onAfterSaveChangesInvoke(afterSaveChangesEventArgs);
    }

    private void handleCounters(ObjectNode batchResult) {

        String docId = getStringField(batchResult, CommandType.COUNTERS, "Id");

        ObjectNode countersDetail = (ObjectNode) batchResult.get("CountersDetail");
        if (countersDetail == null) {
            throwMissingField(CommandType.COUNTERS, "CountersDetail");
        }

        ArrayNode counters = (ArrayNode) countersDetail.get("Counters");
        if (counters == null) {
            throwMissingField(CommandType.COUNTERS, "Counters");
        }

        Tuple<Boolean, Map<String, Long>> cache = _session.getCountersByDocId().get(docId);
        if (cache == null) {
            cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
            _session.getCountersByDocId().put(docId, cache);
        }

        for (JsonNode counter : counters) {
            JsonNode name = counter.get("CounterName");
            JsonNode value = counter.get("TotalValue");

            if (name != null && !name.isNull() && value != null && !value.isNull()) {
                cache.second.put(name.asText(), value.longValue());
            }
        }
    }

    private static String getStringField(ObjectNode json, CommandType type, String fieldName) {
        JsonNode jsonNode = json.get(fieldName);
        if (jsonNode == null || jsonNode.isNull()) {
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

    private static void throwMissingField(CommandType type, String fieldName) {
        throw new IllegalStateException(type + " response is invalid. Field '" + fieldName + "' is missing.");
    }

    private static void throwOnNullResults() {
        throw new IllegalStateException("Received empty response from the server. This is not supposed to happen and is likely a bug.");
    }

}
