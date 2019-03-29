package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.serverwide.operations.NodeId;

public abstract class OngoingTask {

    private long taskId;
    private OngoingTaskType taskType;
    private NodeId responsibleNode;
    private OngoingTaskState taskState;
    private OngoingTaskConnectionStatus taskConnectionStatus;
    private String taskName;
    private String error;

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public OngoingTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(OngoingTaskType taskType) {
        this.taskType = taskType;
    }

    public NodeId getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(NodeId responsibleNode) {
        this.responsibleNode = responsibleNode;
    }

    public OngoingTaskState getTaskState() {
        return taskState;
    }

    public void setTaskState(OngoingTaskState taskState) {
        this.taskState = taskState;
    }

    public OngoingTaskConnectionStatus getTaskConnectionStatus() {
        return taskConnectionStatus;
    }

    public void setTaskConnectionStatus(OngoingTaskConnectionStatus taskConnectionStatus) {
        this.taskConnectionStatus = taskConnectionStatus;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
