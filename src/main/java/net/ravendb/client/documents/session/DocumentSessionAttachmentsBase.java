package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.*;
import net.ravendb.client.documents.operations.attachments.AttachmentName;
import net.ravendb.client.extensions.JsonExtensions;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;

public abstract class DocumentSessionAttachmentsBase extends AdvancedSessionExtentionBase {
    protected DocumentSessionAttachmentsBase(InMemoryDocumentSessionOperations session) {
        super(session);
    }

    @SuppressWarnings("ConstantConditions")
    public AttachmentName[] getNames(Object entity) {
        if (entity == null) {
            return new AttachmentName[0];
        }

        DocumentInfo document = documentsByEntity.get(entity);
        if (document == null) {
            throwEntityNotInSession(entity);
        }

        JsonNode attachments = document.getMetadata().get(Constants.Documents.Metadata.ATTACHMENTS);
        if (attachments == null) {
            return new AttachmentName[0];
        }

        AttachmentName[] results = new AttachmentName[attachments.size()];
        for (int i = 0; i < attachments.size(); i++) {
            JsonNode jsonNode = attachments.get(i);
            results[i] = JsonExtensions.getDefaultMapper().convertValue(jsonNode, AttachmentName.class);
        }
        return results;
    }

    public void store(String documentId, String name, InputStream stream) {
        store(documentId, name, stream, null);
    }

    public void store(String documentId, String name, InputStream stream, String contentType) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.DELETE, null))) {
            throwOtherDeferredCommandException(documentId, name, "store", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_PUT, name))) {
            throwOtherDeferredCommandException(documentId, name, "store", "create");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_DELETE, name))) {
            throwOtherDeferredCommandException(documentId, name, "store", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_MOVE, name))) {
            throwOtherDeferredCommandException(documentId, name, "store", "rename");
        }

        DocumentInfo documentInfo = documentsById.getValue(documentId);
        if (documentInfo != null && deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeleted(documentId, name, "store", null, documentId);
        }

        defer(new PutAttachmentCommandData(documentId, name, stream, contentType, null));
    }

    public void store(Object entity, String name, InputStream stream) {
        store(entity, name, stream, null);
    }

    @SuppressWarnings("ConstantConditions")
    public void store(Object entity, String name, InputStream stream, String contentType) {
        DocumentInfo document = documentsByEntity.get(entity);
        if (document == null) {
            throwEntityNotInSession(entity);
        }

        store(document.getId(), name, stream, contentType);
    }

    protected void throwEntityNotInSession(Object entity) {
        throw new IllegalArgumentException(entity + " is not associated with the session. Use documentId instead or track the entity in the session.");
    }


    @SuppressWarnings("ConstantConditions")
    public void delete(Object entity, String name) {
        DocumentInfo document = documentsByEntity.get(entity);

        if (document == null) {
            throwEntityNotInSession(entity);
        }

        delete(document.getId(), name);
    }

    public void delete(String documentId, String name) {
        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.DELETE, null)) ||
                deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_DELETE, name))) {
            return; // no-op
        }

        DocumentInfo documentInfo = documentsById.getValue(documentId);
        if (documentInfo != null && deletedEntities.contains(documentInfo.getEntity())) {
            return;  //no-op
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_PUT, name))) {
            throwOtherDeferredCommandException(documentId, name, "delete", "create");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_MOVE, name))) {
            throwOtherDeferredCommandException(documentId, name, "delete", "rename");
        }

        defer(new DeleteAttachmentCommandData(documentId, name, null));
    }

    public void rename(String documentId, String name, String newName) {
        move(documentId, name, documentId, newName);
    }

    public void rename(Object entity, String name, String newName) {
        move(entity, name, entity, newName);
    }

    public void move(Object sourceEntity, String sourceName, Object destinationEntity, String destinationName) {
        if (sourceEntity == null) {
            throw new IllegalArgumentException("SourceEntity cannot be null");
        }

        if (destinationEntity == null) {
            throw new IllegalArgumentException("DestinationEntity cannot be null");
        }

        DocumentInfo sourceDocument = documentsByEntity.get(sourceEntity);
        if (sourceDocument == null) {
            throwEntityNotInSession(sourceEntity);
        }

        DocumentInfo destinationDocument = documentsByEntity.get(destinationEntity);
        if (destinationDocument == null) {
            throwEntityNotInSession(destinationEntity);
        }

        move(sourceDocument.getId(), sourceName, destinationDocument.getId(), destinationName);
    }

    public void move(String sourceDocumentId, String sourceName, String destinationDocumentId, String destinationName) {
        if (StringUtils.isBlank(sourceDocumentId)) {
            throw new IllegalArgumentException("SourceDocumentId is required");
        }

        if (StringUtils.isBlank(sourceName)) {
            throw new IllegalArgumentException("SourceName is required");
        }

        if (StringUtils.isBlank(destinationDocumentId)) {
            throw new IllegalArgumentException("DestinationDocumentId is required");
        }

        if (StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("DestinationName is required");
        }

        if (sourceDocumentId.equalsIgnoreCase(destinationDocumentId) && sourceName.equals(destinationName)) {
            return; // no-op
        }

        DocumentInfo sourceDocument = documentsById.getValue(sourceDocumentId);
        if (sourceDocument != null && deletedEntities.contains(sourceDocument.getEntity())) {
            throwDocumentAlreadyDeleted(sourceDocumentId, sourceName, "move", destinationDocumentId, sourceDocumentId);
        }

        DocumentInfo destinationDocument = documentsById.getValue(destinationDocumentId);
        if (destinationDocument != null && deletedEntities.contains(destinationDocument.getEntity())) {
            throwDocumentAlreadyDeleted(sourceDocumentId, sourceName, "move", destinationDocumentId, destinationDocumentId);
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(sourceDocumentId, CommandType.ATTACHMENT_DELETE, sourceName))) {
            throwOtherDeferredCommandException(sourceDocumentId, sourceName, "rename", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(sourceDocumentId, CommandType.ATTACHMENT_MOVE, sourceName))) {
            throwOtherDeferredCommandException(sourceDocumentId, sourceName, "rename", "rename");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(destinationDocumentId, CommandType.ATTACHMENT_DELETE, destinationName))) {
            throwOtherDeferredCommandException(sourceDocumentId, destinationName, "rename", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(destinationDocumentId, CommandType.ATTACHMENT_MOVE, destinationName))) {
            throwOtherDeferredCommandException(sourceDocumentId, destinationName, "rename", "rename");
        }

        defer(new MoveAttachmentCommandData(sourceDocumentId, sourceName, destinationDocumentId, destinationName, null));
    }

    public void copy(Object sourceEntity, String sourceName, Object destinationEntity, String destinationName) {
        if (sourceEntity == null) {
            throw new IllegalArgumentException("SourceEntity is null");
        }

        if (destinationEntity == null) {
            throw new IllegalArgumentException("DestinationEntity is null");
        }

        DocumentInfo sourceDocument = documentsByEntity.get(sourceEntity);
        if (sourceDocument == null) {
            throwEntityNotInSession(sourceEntity);
        }

        DocumentInfo destinationDocument = documentsByEntity.get(destinationEntity);
        if (destinationDocument == null) {
            throwEntityNotInSession(destinationEntity);
        }

        copy(sourceDocument.getId(), sourceName, destinationDocument.getId(), destinationName);
    }

    public void copy(String sourceDocumentId, String sourceName, String destinationDocumentId, String destinationName) {
        if (StringUtils.isBlank(sourceDocumentId)) {
            throw new IllegalArgumentException("SourceDocumentId is required");
        }

        if (StringUtils.isBlank(sourceName)) {
            throw new IllegalArgumentException("SourceName is required");
        }

        if (StringUtils.isBlank(destinationDocumentId)) {
            throw new IllegalArgumentException("DestinationDocumentId is required");
        }

        if (StringUtils.isBlank(destinationName)) {
            throw new IllegalArgumentException("DestinationName is required");
        }


        if (sourceDocumentId.equalsIgnoreCase(destinationDocumentId) && sourceName.equals(destinationName)) {
            return; // no-op
        }

        DocumentInfo sourceDocument = documentsById.getValue(sourceDocumentId);
        if (sourceDocument != null && deletedEntities.contains(sourceDocument.getEntity())) {
            throwDocumentAlreadyDeleted(sourceDocumentId, sourceName, "copy", destinationDocumentId, sourceDocumentId);
        }

        DocumentInfo destinationDocument = documentsById.getValue(destinationDocumentId);
        if (destinationDocument != null && deletedEntities.contains(destinationDocument.getEntity())) {
            throwDocumentAlreadyDeleted(sourceDocumentId, sourceName, "copy", destinationDocumentId, destinationDocumentId);
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(sourceDocumentId, CommandType.ATTACHMENT_DELETE, sourceName))) {
            throwOtherDeferredCommandException(sourceDocumentId, sourceName, "copy", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(sourceDocumentId, CommandType.ATTACHMENT_MOVE, sourceName))) {
            throwOtherDeferredCommandException(sourceDocumentId, sourceName, "copy", "rename");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(destinationDocumentId, CommandType.ATTACHMENT_DELETE, destinationName))) {
            throwOtherDeferredCommandException(destinationDocumentId, destinationName, "copy", "delete");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(destinationDocumentId, CommandType.ATTACHMENT_MOVE, destinationName))) {
            throwOtherDeferredCommandException(destinationDocumentId, destinationName, "copy", "rename");
        }

        defer(new CopyAttachmentCommandData(sourceDocumentId, sourceName, destinationDocumentId, destinationName, null));
    }

    private static void throwDocumentAlreadyDeleted(String documentId, String name, String operation, String destinationDocumentId, String deletedDocumentId) {
        throw new IllegalStateException("Can't " + operation + " attachment '" + name + "' of document '" + documentId + "' " +
                (destinationDocumentId != null ? " to '" + destinationDocumentId + "'" : "") +
                ", the document '" + deletedDocumentId + "' was already deleted in this session");
    }

    private static void throwOtherDeferredCommandException(String documentId, String name, String operation, String previousOperation) {
        throw new IllegalStateException("Can't " + operation + " attachment '" + name + "' of document '"
                + documentId + "', there is a deferred command registered to " + previousOperation + " an attachment with '" + name + "' name.");
    }

}
