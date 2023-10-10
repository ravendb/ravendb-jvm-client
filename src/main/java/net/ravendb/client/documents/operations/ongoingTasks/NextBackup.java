package net.ravendb.client.documents.operations.ongoingTasks;

import java.time.Duration;
import java.util.Date;

public class NextBackup {
    private Duration timeSpan;
    private Date dateTime;
    private boolean isFull;

    private Date originalBackupTime;

    public Duration getTimeSpan() {
        return timeSpan;
    }

    public void setTimeSpan(Duration timeSpan) {
        this.timeSpan = timeSpan;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    public Date getOriginalBackupTime() {
        return originalBackupTime;
    }

    public void setOriginalBackupTime(Date originalBackupTime) {
        this.originalBackupTime = originalBackupTime;
    }
}
