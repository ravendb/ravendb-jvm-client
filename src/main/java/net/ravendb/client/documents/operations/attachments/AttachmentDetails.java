package net.ravendb.client.documents.operations.attachments;

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
