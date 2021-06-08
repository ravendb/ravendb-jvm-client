package net.ravendb.client.documents.operations.backups;

public class PeriodicBackupConfiguration extends BackupConfiguration {

    private String name;
    private long taskId;
    private boolean disabled;
    private String mentorNode;

    private RetentionPolicy retentionPolicy;

    private String fullBackupFrequency;
    private String incrementalBackupFrequency;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public RetentionPolicy getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public String getFullBackupFrequency() {
        return fullBackupFrequency;
    }

    public void setFullBackupFrequency(String fullBackupFrequency) {
        this.fullBackupFrequency = fullBackupFrequency;
    }

    public String getIncrementalBackupFrequency() {
        return incrementalBackupFrequency;
    }

    public void setIncrementalBackupFrequency(String incrementalBackupFrequency) {
        this.incrementalBackupFrequency = incrementalBackupFrequency;
    }
}
