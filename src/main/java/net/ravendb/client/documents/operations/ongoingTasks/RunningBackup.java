package net.ravendb.client.documents.operations.ongoingTasks;

import java.util.Date;

public class RunningBackup {
    private Date startTime;
    private boolean isFull;
    private Long runningBackupTaskId;

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    public Long getRunningBackupTaskId() {
        return runningBackupTaskId;
    }

    public void setRunningBackupTaskId(Long runningBackupTaskId) {
        this.runningBackupTaskId = runningBackupTaskId;
    }
}
