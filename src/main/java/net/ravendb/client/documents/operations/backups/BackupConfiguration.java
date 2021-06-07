package net.ravendb.client.documents.operations.backups;

public class BackupConfiguration {

    private BackupType backupType;
    private SnapshotSettings snapshotSettings;
    private BackupEncryptionSettings backupEncryptionSettings;

    private LocalSettings localSettings;
    private S3Settings s3Settings;
    private GlacierSettings glacierSettings;
    private AzureSettings azureSettings;
    private FtpSettings ftpSettings;
    private GoogleCloudSettings googleCloudSettings;

    public BackupType getBackupType() {
        return backupType;
    }

    public void setBackupType(BackupType backupType) {
        this.backupType = backupType;
    }

    public SnapshotSettings getSnapshotSettings() {
        return snapshotSettings;
    }

    public void setSnapshotSettings(SnapshotSettings snapshotSettings) {
        this.snapshotSettings = snapshotSettings;
    }

    public BackupEncryptionSettings getBackupEncryptionSettings() {
        return backupEncryptionSettings;
    }

    public void setBackupEncryptionSettings(BackupEncryptionSettings backupEncryptionSettings) {
        this.backupEncryptionSettings = backupEncryptionSettings;
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

    public GoogleCloudSettings getGoogleCloudSettings() {
        return googleCloudSettings;
    }

    public void setGoogleCloudSettings(GoogleCloudSettings googleCloudSettings) {
        this.googleCloudSettings = googleCloudSettings;
    }
}
