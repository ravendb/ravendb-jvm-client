package net.ravendb.client.serverwide.operations;

public class DeleteDatabaseResult {
    private long raftCommandIndex;
    private String[] pendingDeletes;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    public String[] getPendingDeletes() {
        return pendingDeletes;
    }

    public void setPendingDeletes(String[] pendingDeletes) {
        this.pendingDeletes = pendingDeletes;
    }
}
