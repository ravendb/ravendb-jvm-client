package net.ravendb.client.documents.operations.backups;

public class PeriodicBackupConfiguration {

    private long taskId;
    private boolean disabled;
    private String name;
    private String mentorNode;
    private BackupType backupType;
    private BackupEncryptionSettings backupEncryptionSettings;
    private String fullBackupFrequency;
    private String incrementalBackupFrequency;
    private LocalSettings localSettings;
    private S3Settings s3Settings;
    private GlacierSettings glacierSettings;
    private AzureSettings azureSettings;
    private FtpSettings ftpSettings;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMentorNode() {
        return mentorNode;
    }

    public void setMentorNode(String mentorNode) {
        this.mentorNode = mentorNode;
    }

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public BackupEncryptionSettings getBackupEncryptionSettings() {
        return backupEncryptionSettings;
    }

    public void setBackupEncryptionSettings(BackupEncryptionSettings backupEncryptionSettings) {
        this.backupEncryptionSettings = backupEncryptionSettings;
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

    public LocalSettings getLocalSettings() {
        return localSettings;
    }

    public void setLocalSettings(LocalSettings localSettings) {
        this.localSettings = localSettings;
    }

    public S3Settings getS3Settings() {
        return s3Settings;
    }

    public void setS3Settings(S3Settings s3Settings) {
        this.s3Settings = s3Settings;
    }

    public GlacierSettings getGlacierSettings() {
        return glacierSettings;
    }

    public void setGlacierSettings(GlacierSettings glacierSettings) {
        this.glacierSettings = glacierSettings;
    }

    public AzureSettings getAzureSettings() {
        return azureSettings;
    }

    public void setAzureSettings(AzureSettings azureSettings) {
        this.azureSettings = azureSettings;
    }

    public FtpSettings getFtpSettings() {
        return ftpSettings;
    }

    public void setFtpSettings(FtpSettings ftpSettings) {
        this.ftpSettings = ftpSettings;
    }
}
