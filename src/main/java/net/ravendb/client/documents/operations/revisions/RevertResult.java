package net.ravendb.client.documents.operations.revisions;

public class RevertResult extends OperationResult {
    private int revertedDocuments;

    public int getRevertedDocuments() {
        return revertedDocuments;
    }

    public void setRevertedDocuments(int revertedDocuments) {
        this.revertedDocuments = revertedDocuments;
    }
}
