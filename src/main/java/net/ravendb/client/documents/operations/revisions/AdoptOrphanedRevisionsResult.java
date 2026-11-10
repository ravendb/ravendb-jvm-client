package net.ravendb.client.documents.operations.revisions;

public final class AdoptOrphanedRevisionsResult extends OperationResult {

    private int adoptedCount;

    public int getAdoptedCount() {
        return adoptedCount;
    }

    public void setAdoptedCount(int adoptedCount) {
        this.adoptedCount = adoptedCount;
    }

    @Override
    public boolean canMerge() {
        return true;
    }
}
