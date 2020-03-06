package net.ravendb.client.documents.operations.connectionStrings;

public class RemoveConnectionStringResult {
    private long raftCommandIndex;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
