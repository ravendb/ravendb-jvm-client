package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.batches.BatchCommand;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.session.AfterSaveChangesEventArgs;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.ClientVersionMismatchException;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.JsonArrayResult;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BatchOperation {

    private final InMemoryDocumentSessionOperations _session;

    public BatchOperation(InMemoryDocumentSessionOperations session) {
        this._session = session;
    }

    private List<Object> _entities;
    private int _sessionCommandsCount;
    private int _allCommandsCount;

    private Map<String, DocumentInfo> _modifications;

    public BatchCommand createRequest() {
        InMemoryDocumentSessionOperations.SaveChangesData result = _session.prepareForSaveChanges();
        _sessionCommandsCount = result.getSessionCommands().size();
        result.getSessionCommands().addAll(result.getDeferredCommands());
        _allCommandsCount = result.getSessionCommands().size();

        if (_allCommandsCount == 0) {
            return null;
        }

        _session.validateClusterTransaction(result);
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
        /* TODO
         if (documentInfo.Metadata.Modifications == null)
                return;

            documentInfo.MetadataInstance = null;
            using (var old = documentInfo.Metadata)
            {
                documentInfo.Metadata = _session.Context.ReadObject(documentInfo.Metadata, id);
                documentInfo.Metadata.Modifications = null;
            }

            documentInfo.Document.Modifications = new DynamicJsonValue(documentInfo.Document)
            {
                [Constants.Documents.Metadata.Key] = documentInfo.Metadata
            };

            using (var old = documentInfo.Document)
            {
                documentInfo.Document = _session.Context.ReadObject(documentInfo.Document, id);
                documentInfo.Document.Modifications = null;
            }
         */
    }

    /* TODO
        private DocumentInfo GetOrAddModifications(LazyStringValue id, DocumentInfo documentInfo, bool applyModifications)
        {
            if (_modifications == null)
                _modifications = new Dictionary<LazyStringValue, DocumentInfo>(LazyStringValueComparer.Instance);

            if (_modifications.TryGetValue(id, out var modifiedDocumentInfo))
            {
                if (applyModifications)
                    ApplyMetadataModifications(id, modifiedDocumentInfo);
            }
            else
                _modifications[id] = modifiedDocumentInfo = documentInfo;

            return modifiedDocumentInfo;
        }
        */

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

        /* TODO
        DocumentInfo documentInfo = getOrAllModifications(id, sessionDocumentInfo, true);
         var documentInfo = GetOrAddModifications(id, sessionDocumentInfo, applyModifications: true);

            if (documentInfo.Metadata.TryGet(Constants.Documents.Metadata.Attachments, out BlittableJsonReaderArray attachmentsJson) == false || attachmentsJson == null ||
                attachmentsJson.Length == 0)
                return;

            var name = GetLazyStringField(batchResult, type, attachmentNameFieldName);

            if (documentInfo.Metadata.Modifications == null)
                documentInfo.Metadata.Modifications = new DynamicJsonValue(documentInfo.Metadata);

            var attachments = new DynamicJsonArray();
            documentInfo.Metadata.Modifications[Constants.Documents.Metadata.Attachments] = attachments;

            foreach (BlittableJsonReaderObject attachment in attachmentsJson)
            {
                var attachmentName = GetLazyStringField(attachment, type, nameof(AttachmentDetails.Name));
                if (attachmentName == name)
                    continue;

                attachments.Add(attachment);
                break;
            }
            */
    }

    private void handleAttachmentPut(ObjectNode batchResult) {
        handleAttachmentPutInternal(batchResult, CommandType.ATTACHMENT_PUT, "Id", "Name");
    }

    private void handleAttachmentPutInternal(ObjectNode batchResult, CommandType type, String idFieldName, String attachmentNameFieldName) {
        /* TODO
        var id = GetLazyStringField(batchResult, type, idFieldName);

            if (_session.DocumentsById.TryGetValue(id, out var sessionDocumentInfo) == false)
                return;

            var documentInfo = GetOrAddModifications(id, sessionDocumentInfo, applyModifications: false);

            if (documentInfo.Metadata.Modifications == null)
                documentInfo.Metadata.Modifications = new DynamicJsonValue(documentInfo.Metadata);

            var attachments = documentInfo.Metadata.Modifications[Constants.Documents.Metadata.Attachments] as DynamicJsonArray;
            if (attachments == null)
            {
                attachments = documentInfo.Metadata.TryGet(Constants.Documents.Metadata.Attachments, out BlittableJsonReaderArray attachmentsJson)
                    ? new DynamicJsonArray(attachmentsJson)
                    : new DynamicJsonArray();

                documentInfo.Metadata.Modifications[Constants.Documents.Metadata.Attachments] = attachments;
            }

            attachments.Add(new DynamicJsonValue
            {
                [nameof(AttachmentDetails.ChangeVector)] = GetLazyStringField(batchResult, type, nameof(AttachmentDetails.ChangeVector)),
                [nameof(AttachmentDetails.ContentType)] = GetLazyStringField(batchResult, type, nameof(AttachmentDetails.ContentType)),
                [nameof(AttachmentDetails.Hash)] = GetLazyStringField(batchResult, type, nameof(AttachmentDetails.Hash)),
                [nameof(AttachmentDetails.Name)] = GetLazyStringField(batchResult, type, attachmentNameFieldName),
                [nameof(AttachmentDetails.Size)] = GetLongField(batchResult, type, nameof(AttachmentDetails.Size))
            });
         */
    }

    private void handlePatch(ObjectNode batchResult) {
        /* TODO
        if (batchResult.TryGet(nameof(PatchStatus), out string statusAsString) == false)
                ThrowMissingField(CommandType.PATCH, nameof(PatchStatus));

            if (Enum.TryParse(statusAsString, ignoreCase: true, out PatchStatus status) == false)
                ThrowMissingField(CommandType.PATCH, nameof(PatchStatus));

            switch (status)
            {
                case PatchStatus.Created:
                case PatchStatus.Patched:
                    if (batchResult.TryGet(nameof(PatchResult.ModifiedDocument), out BlittableJsonReaderObject document) == false)
                        return;

                    var id = GetLazyStringField(batchResult, CommandType.PATCH, nameof(ICommandData.Id));

                    if (_session.DocumentsById.TryGetValue(id, out var sessionDocumentInfo) == false)
                        return;

                    var documentInfo = GetOrAddModifications(id, sessionDocumentInfo, applyModifications: true);

                    var changeVector = GetLazyStringField(batchResult, CommandType.PATCH, nameof(Constants.Documents.Metadata.ChangeVector));
                    var lastModified = GetLazyStringField(batchResult, CommandType.PATCH, nameof(Constants.Documents.Metadata.LastModified));

                    documentInfo.ChangeVector = changeVector;

                    documentInfo.Metadata.Modifications = new DynamicJsonValue(documentInfo.Metadata)
                    {
                        [Constants.Documents.Metadata.Id] = id,
                        [Constants.Documents.Metadata.ChangeVector] = changeVector,
                        [Constants.Documents.Metadata.LastModified] = lastModified
                    };

                    using (var old = documentInfo.Document)
                    {
                        documentInfo.Document = document;

                        ApplyMetadataModifications(id, documentInfo);
                    }

                    if (documentInfo.Entity != null)
                        _session.EntityToBlittable.PopulateEntity(documentInfo.Entity, id, documentInfo.Document, _session.JsonSerializer);

                    break;
            }
         */
    }

    private void handleDelete(ObjectNode batchReslt) {
        handleDeleteInternal(batchReslt, CommandType.DELETE);
    }

    private void handleDeleteInternal(ObjectNode batchResult, CommandType type) {
        /* TODO
        var id = GetLazyStringField(batchResult, type, nameof(ICommandData.Id));

            _modifications?.Remove(id);

            if (_session.DocumentsById.TryGetValue(id, out var documentInfo) == false)
                return;

            _session.DocumentsById.Remove(id);

            if (documentInfo.Entity != null)
            {
                _session.DocumentsByEntity.Remove(documentInfo.Entity);
                _session.DeletedEntities.Remove(documentInfo.Entity);
            }
         */
    }

    private void handlePut(int index, ObjectNode batchResult, boolean isDeferred) {
        /* TODO
        object entity = null;
            DocumentInfo documentInfo = null;
            if (isDeferred == false)
            {
                entity = _entities[index];
                if (_session.DocumentsByEntity.TryGetValue(entity, out documentInfo) == false)
                    return;
            }

            var id = GetLazyStringField(batchResult, CommandType.PUT, Constants.Documents.Metadata.Id);
            var changeVector = GetLazyStringField(batchResult, CommandType.PUT, Constants.Documents.Metadata.ChangeVector);

            if (isDeferred)
            {
                if (_session.DocumentsById.TryGetValue(id, out var sessionDocumentInfo) == false)
                    return;

                documentInfo = GetOrAddModifications(id, sessionDocumentInfo, applyModifications: true);

                entity = documentInfo.Entity;
            }

            documentInfo.Metadata.Modifications = new DynamicJsonValue(documentInfo.Metadata);

            foreach (var propertyName in batchResult.GetPropertyNames())
            {
                if (propertyName == nameof(ICommandData.Type))
                    continue;

                documentInfo.Metadata.Modifications[propertyName] = batchResult[propertyName];
            }

            documentInfo.Id = id;
            documentInfo.ChangeVector = changeVector;

            ApplyMetadataModifications(id, documentInfo);

            _session.DocumentsById.Add(documentInfo);

            if (entity != null)
                _session.GenerateEntityIdOnTheClient.TrySetIdentity(entity, id);

            var afterSaveChangesEventArgs = new AfterSaveChangesEventArgs(_session, documentInfo.Id, documentInfo.Entity);
            _session.OnAfterSaveChangesInvoke(afterSaveChangesEventArgs);
         */
    }

    private void handleCounters(ObjectNode batchResult) {
        /* TODO
         var docId = GetLazyStringField(batchResult, CommandType.Counters, nameof(CountersBatchCommandData.Id));

            if (batchResult.TryGet(nameof(CountersDetail), out BlittableJsonReaderObject countersDetail) == false)
                ThrowMissingField(CommandType.Counters, nameof(CountersDetail));

            if (countersDetail.TryGet(nameof(CountersDetail.Counters), out BlittableJsonReaderArray counters) == false)
                ThrowMissingField(CommandType.Counters, nameof(CountersDetail.Counters));

            if (_session.CountersByDocId.TryGetValue(docId, out var cache) == false)
            {
                cache.Values = new Dictionary<string, long?>(StringComparer.OrdinalIgnoreCase);
                _session.CountersByDocId.Add(docId, cache);
            }

            foreach (BlittableJsonReaderObject counter in counters)
            {
                if (counter.TryGet(nameof(CounterDetail.CounterName), out string name) == false ||
                    counter.TryGet(nameof(CounterDetail.TotalValue), out long value) == false)
                    continue;

                cache.Values[name] = value;
            }
         */
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


/* TODO: delete me!
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
            documentInfo.setMetadataInstance(null);

            _session.documentsById.add(documentInfo);
            _session.getGenerateEntityIdOnTheClient().trySetIdentity(entity, id);

            AfterSaveChangesEventArgs afterSaveChangesEventArgs = new AfterSaveChangesEventArgs(_session, documentInfo.getId(), documentInfo.getEntity());
            _session.onAfterSaveChangesInvoke(afterSaveChangesEventArgs);
        }
    }
    */

    private static void throwOnNullResults() {
        throw new IllegalStateException("Received empty response from the server. This is not supposed to happen and is likely a bug.");
    }

}
