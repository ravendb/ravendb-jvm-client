package net.ravendb.client.documents.session;

import net.ravendb.client.documents.attachments.AttachmentType;
import net.ravendb.client.documents.commands.HeadAttachmentCommand;
import net.ravendb.client.documents.operations.attachments.*;

import java.util.List;

/**
 * Implements Unit of Work for accessing the RavenDB server
 */
public class DocumentSessionAttachments extends DocumentSessionAttachmentsBase implements IAttachmentsSessionOperations {

    public DocumentSessionAttachments(InMemoryDocumentSessionOperations session) {
        super(session);
    }

    @Override
    public boolean exists(String documentId, String name) {
        HeadAttachmentCommand command = new HeadAttachmentCommand(documentId, name, null);
        session.incrementRequestCount();
        requestExecutor.execute(command, sessionInfo);
        return command.getResult() != null;
    }

    @Override
    public CloseableAttachmentResult get(String documentId, String name) {
        GetAttachmentOperation operation = new GetAttachmentOperation(documentId, name, AttachmentType.DOCUMENT, null);
        session.incrementRequestCount();
        return session.getOperations().send(operation, sessionInfo);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public CloseableAttachmentResult get(Object entity, String name) {
        DocumentInfo document = session.documentsByEntity.get(entity);
        if (document == null) {
            throwEntityNotInSessionOrMissingId(entity);
        }

        GetAttachmentOperation operation = new GetAttachmentOperation(document.getId(), name, AttachmentType.DOCUMENT, null);
        session.incrementRequestCount();
        return session.getOperations().send(operation, sessionInfo);
    }

    @Override
    public CloseableAttachmentsResult get(List<AttachmentRequest> attachments) {
        GetAttachmentsOperation operation = new GetAttachmentsOperation(attachments, AttachmentType.DOCUMENT);
        return session.getOperations().send(operation, sessionInfo);
    }

    @Override
    public CloseableAttachmentResult getRevision(String documentId, String name, String changeVector) {
        GetAttachmentOperation operation = new GetAttachmentOperation(documentId, name, AttachmentType.REVISION, changeVector);
        session.incrementRequestCount();
        return session.getOperations().send(operation, sessionInfo);
    }
}
