package net.ravendb.client.serverwide.operations;

public class ModifyDatabaseTopologyResult {
    private long raftCommandIndex;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
