package net.ravendb.client.documents.operations.attachments;

/**
 * Contains details about an attachment, including its change vector and associated document ID.
 *
 * <p>This class inherits from {@link AttachmentName} and provides additional metadata
 * necessary for managing attachment operations.</p>
 */

public class AttachmentDetails extends AttachmentName {
    /**
     * The change vector of the attachment for concurrency control.
     */
    private String changeVector;
    /**
     * The ID of the document associated with the attachment.
     */
    private String documentId;

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
