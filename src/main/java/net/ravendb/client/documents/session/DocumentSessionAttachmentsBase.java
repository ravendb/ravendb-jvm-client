package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.DeleteAttachmentCommandData;
import net.ravendb.client.documents.commands.batches.PutAttachmentCommandData;
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
            throw new IllegalStateException("Cannot store attachment" + name + " of document " + documentId + ", there is a deferred command registered for this document to be deleted");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_PUT, name))) {
            throw new IllegalStateException("Cannot store attachment" + name + " of document " + documentId + ", there is a deferred command registered to create an attachment with the same name.");
        }

        if (deferredCommandsMap.containsKey(IdTypeAndName.create(documentId, CommandType.ATTACHMENT_DELETE, name))) {
            throw new IllegalStateException("Cannot store attachment" + name + " of document " + documentId + ", there is a deferred command registered to delete an attachment with the same name.");
        }

        DocumentInfo documentInfo = documentsById.getValue(documentId);
        if (documentInfo != null && deletedEntities.contains(documentInfo.getEntity())) {
            throw new IllegalStateException("Cannot store attachment " + name + " of document " + documentId + ", the document was already deleted in this session.");
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
            throw new IllegalStateException("Cannot delete attachment " + name + " of document " + documentId + ", there is a deferred command registered to create an attachment with the same name.");
        }

        defer(new DeleteAttachmentCommandData(documentId, name, null));
    }

}
