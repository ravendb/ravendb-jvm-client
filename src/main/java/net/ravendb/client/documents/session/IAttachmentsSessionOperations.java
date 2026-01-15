package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.attachments.AttachmentRequest;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentsResult;

import java.util.List;

/**
 * Advanced synchronous session operations for working with attachments.
 */
public interface IAttachmentsSessionOperations extends IAttachmentsSessionOperationsBase {

    /**
     * Checks whether an attachment exists for the specified document ID and attachment name.
     * @param documentId Document Id
     * @param name Attachment name
     * @return true, if attachment exists
     */
    boolean exists(String documentId, String name);

    /**
     * Returns the attachment for the specified document ID and attachment name.
     * @param documentId Document Id
     * @param name Name of attachment
     * @return Attachment
     */
    CloseableAttachmentResult get(String documentId, String name);

    /**
     * Returns the attachment for the specified entity instance and attachment name.
     * @param entity Entity
     * @param name Name of attachment
     * @return Attachment
     */
    CloseableAttachmentResult get(Object entity, String name);

    /**
     * Returns an enumerator over multiple attachments. Each result includes the attachment stream and metadata.
     * @param attachments Attachments to get
     * @return attachments
     */
    CloseableAttachmentsResult get(List<AttachmentRequest> attachments);

    /**
     * Returns the attachment from a document revision for the specified document ID, attachment name, and change vector.
     * @param documentId Document Id
     * @param name Name of attachment
     * @param changeVector Change vector
     * @return Attachment
     */
    CloseableAttachmentResult getRevision(String documentId, String name, String changeVector);


}
