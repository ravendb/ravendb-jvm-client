package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.attachments.AttachmentName;

import java.io.InputStream;

public interface IAttachmentsSessionOperationsBase {
    /**
     * Returns the attachments info of a document.
     * @param entity Entity to use
     * @return attachments names
     */
    AttachmentName[] getNames(Object entity);

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

    /**
     * Marks the specified document's attachment for rename. The attachment will be renamed when saveChanges is called.
     * @param entity instance of entity of the document which holds the attachment
     * @param name the attachment name
     * @param newName the attachment new name
     */
    void rename(Object entity, String name, String newName);

    /**
     * Marks the specified document's attachment for rename. The attachment will be renamed when saveChanges is called.
     * @param documentId the document which holds the attachment
     * @param name the attachment name
     * @param newName the attachment new name
     */
    void rename(String documentId, String name, String newName);

    /**
     * Copies specified source document attachment to destination document. The operation will be executed when saveChanges is called.
     * @param sourceEntity the document which holds the attachment
     * @param sourceName the attachment name
     * @param destinationEntity the document to which the attachment will be copied
     * @param destinationName the attachment name
     */
    void copy(Object sourceEntity, String sourceName, Object destinationEntity, String destinationName);

    /**
     * Copies specified source document attachment to destination document. The operation will be executed when saveChanges is called.
     * @param sourceDocumentId the document which holds the attachment
     * @param sourceName the attachment name
     * @param destinationDocumentId the document to which the attachment will be copied
     * @param destinationName the attachment name
     */
    void copy(String sourceDocumentId, String sourceName, String destinationDocumentId, String destinationName);

    /**
     * Moves specified source document attachment to destination document. The operation will be executed when saveChanges is called.
     * @param sourceEntity the document which holds the attachment
     * @param sourceName the attachment name
     * @param destinationEntity the document to which the attachment will be moved
     * @param destinationName the attachment name
     */
    void move(Object sourceEntity, String sourceName, Object destinationEntity, String destinationName);

    /**
     * Moves specified source document attachment to destination document. The operation will be executed when saveChanges is called.
     * @param sourceDocumentId the document which holds the attachment
     * @param sourceName the attachment name
     * @param destinationDocumentId the document to which the attachment will be moved
     * @param destinationName the attachment name
     */
    void move(String sourceDocumentId, String sourceName, String destinationDocumentId, String destinationName);
}
