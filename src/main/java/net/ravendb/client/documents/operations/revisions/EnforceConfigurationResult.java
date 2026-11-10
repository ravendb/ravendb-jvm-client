package net.ravendb.client.documents.operations.revisions;

public final class EnforceConfigurationResult extends OperationResult {

    private int removedRevisions;

    public int getRemovedRevisions() {
        return removedRevisions;
    }

    public void setRemovedRevisions(int removedRevisions) {
        this.removedRevisions = removedRevisions;
    }

    @Override
    public boolean canMerge() {
        return true;
    }
}
