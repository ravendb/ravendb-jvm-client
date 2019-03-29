package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.backups.BackupType;

import java.util.Date;
import java.util.List;

public class OngoingTaskBackup extends OngoingTask {

    public OngoingTaskBackup() {
        setTaskType(OngoingTaskType.BACKUP);
    }

    private BackupType backupType;
    private List<String> backupDestinations;
    private Date lastFullBackup;
    private Date lastIncrementalBackup;
    private RunningBackup onGoingBackup;
    private NextBackup nextBackup;

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public List<String> getBackupDestinations() {
        return backupDestinations;
    }

    public void setBackupDestinations(List<String> backupDestinations) {
        this.backupDestinations = backupDestinations;
    }

    public Date getLastFullBackup() {
        return lastFullBackup;
    }

    public void setLastFullBackup(Date lastFullBackup) {
        this.lastFullBackup = lastFullBackup;
    }

    public Date getLastIncrementalBackup() {
        return lastIncrementalBackup;
    }

    public void setLastIncrementalBackup(Date lastIncrementalBackup) {
        this.lastIncrementalBackup = lastIncrementalBackup;
    }

    public RunningBackup getOnGoingBackup() {
        return onGoingBackup;
    }

    public void setOnGoingBackup(RunningBackup onGoingBackup) {
        this.onGoingBackup = onGoingBackup;
    }

    public NextBackup getNextBackup() {
        return nextBackup;
    }

    public void setNextBackup(NextBackup nextBackup) {
        this.nextBackup = nextBackup;
    }
}
