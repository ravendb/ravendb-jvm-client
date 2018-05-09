package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.attachments.AttachmentName;
import net.ravendb.client.documents.operations.attachments.AttachmentResult;

import java.io.InputStream;

/**
 * Attachments advanced synchronous session operations
 */
public interface IAttachmentsSessionOperations {

    /**
     * Returns the attachments info of a document.
     * @param entity Entity to use
     * @return attachments names
     */
    AttachmentName[] getNames(Object entity);

    /**
     * Check if attachment exists
     * @param documentId Document Id
     * @param name Attachment name
     * @return true, if attachment exists
     */
    boolean exists(String documentId, String name);

    /**
     * Returns the attachment by the document id and attachment name.
     * @param documentId Document Id
     * @param name Name of attachment
     * @return Attachment
     */
    AttachmentResult get(String documentId, String name);

    /**
     * Returns the attachment by the entity and attachment name.
     * @param entity Entity
     * @param name Name of attachment
     * @return Attachment
     */
    AttachmentResult get(Object entity, String name);

    //TBD AttachmentResult GetRevision(string documentId, string name, string changeVector);

    /**
     * Stores attachment to be sent in the session.
     * @param documentId Document Id
     * @param name Name of attachment
     * @param stream Attachment stream
     */
    void store(String documentId, String name, InputStream stream);

    /**
     * Stores attachment to be sent in the session.
     * @param documentId Document Id
     * @param name Name of attachment
     * @param stream Attachment stream
     * @param contentType Content type
     */
    void store(String documentId, String name, InputStream stream, String contentType);


    /**
     * Stores attachment to be sent in the session.
     * @param entity Entity
     * @param name Name of attachment
     * @param stream Attachment stream
     */
    void store(Object entity, String name, InputStream stream);

    /**
     * Stores attachment to be sent in the session.
     * @param entity Entity
     * @param name Name of attachment
     * @param stream Attachment stream
     * @param contentType Content type
     */
    void store(Object entity, String name, InputStream stream, String contentType);

    /**
     * Marks the specified document's attachment for deletion. The attachment will be deleted when
     * saveChanges is called.
     * @param documentId the document which holds the attachment
     * @param name the attachment name
     */
    void delete(String documentId, String name);

    /**
     * Marks the specified document's attachment for deletion. The attachment will be deleted when
     * saveChanges is called.
     * @param entity instance of entity of the document which holds the attachment
     * @param name the attachment name
     */
    void delete(Object entity, String name);
}
