package net.ravendb.client.documents.operations.ongoingTasks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class RunningBackup {
    private Date startTime;
    private boolean isFull;
    private long runningBackupTaskId;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @JsonProperty("IsFull")
    public boolean isFull() {
        return isFull;
    }

    @JsonProperty("IsFull")
    public void setFull(boolean full) {
        isFull = full;
    }

    public long getRunningBackupTaskId() {
        return runningBackupTaskId;
    }

    public void setRunningBackupTaskId(long runningBackupTaskId) {
        this.runningBackupTaskId = runningBackupTaskId;
    }
}
