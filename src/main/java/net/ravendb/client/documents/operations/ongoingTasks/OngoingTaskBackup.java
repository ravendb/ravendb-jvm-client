package net.ravendb.client.documents.operations.ongoingTasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.operations.backups.BackupType;
import net.ravendb.client.documents.operations.backups.RetentionPolicy;

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
    private RetentionPolicy retentionPolicy;

    @JsonProperty("IsEncrypted")
    private boolean encrypted;
    private String lastExecutingNodeTag;

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

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getLastExecutingNodeTag() {
        return lastExecutingNodeTag;
    }

    public void setLastExecutingNodeTag(String lastExecutingNodeTag) {
        this.lastExecutingNodeTag = lastExecutingNodeTag;
    }
}
