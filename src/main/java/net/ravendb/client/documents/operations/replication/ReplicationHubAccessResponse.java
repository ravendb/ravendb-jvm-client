package net.ravendb.client.documents.operations.replication;

public class ReplicationHubAccessResponse {
    private long raftCommandIndex;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
