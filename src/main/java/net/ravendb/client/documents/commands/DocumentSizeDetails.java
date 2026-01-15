package net.ravendb.client.documents.commands;

public class DocumentSizeDetails extends SizeDetails {

    private String docId;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
}
