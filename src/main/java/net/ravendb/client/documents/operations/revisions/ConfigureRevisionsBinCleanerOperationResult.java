package net.ravendb.client.documents.operations.revisions;

public final class ConfigureRevisionsBinCleanerOperationResult {
    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
