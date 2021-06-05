package net.ravendb.client.serverwide.operations.ongoingTasks;

public class ServerWideTaskResponse {
    private String name;
    private long raftCommandIndex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
