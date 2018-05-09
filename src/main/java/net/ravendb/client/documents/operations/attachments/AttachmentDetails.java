package net.ravendb.client.documents.operations.attachments;

public class AttachmentDetails extends AttachmentName {
    private String changeVector;
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
