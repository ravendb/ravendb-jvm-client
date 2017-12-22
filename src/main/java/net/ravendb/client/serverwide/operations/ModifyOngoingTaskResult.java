package net.ravendb.client.serverwide.operations;

public class ModifyOngoingTaskResult {
    private long taskId;
    private long raftCommandIndex;
    private String responsibleNode;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    public String getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(String responsibleNode) {
        this.responsibleNode = responsibleNode;
    }
}
