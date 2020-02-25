package net.ravendb.client.serverwide.operations;

public class ConfigureRevisionsForConflictsResult {
    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
