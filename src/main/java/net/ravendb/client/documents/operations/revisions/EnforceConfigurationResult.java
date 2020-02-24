package net.ravendb.client.documents.operations.revisions;

public class EnforceConfigurationResult extends OperationResult {

    private int removedRevisions;

    public int getRemovedRevisions() {
        return removedRevisions;
    }

    public void setRemovedRevisions(int removedRevisions) {
        this.removedRevisions = removedRevisions;
    }
}
