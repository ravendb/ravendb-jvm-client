package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.documents.operations.backups.sharding.ShardedRestoreSettings;

public abstract class RestoreBackupConfigurationBase {
    private String databaseName;
    private String lastFileNameToRestore;
    private String dataDirectory;
    private String encryptionKey;
    private boolean disableOngoingTasks;
    private boolean skipIndexes;

    protected abstract RestoreType getType();

    private ShardedRestoreSettings shardRestoreSettings;
    private BackupEncryptionSettings backupEncryptionSettings;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getLastFileNameToRestore() {
        return lastFileNameToRestore;
    }

    public void setLastFileNameToRestore(String lastFileNameToRestore) {
        this.lastFileNameToRestore = lastFileNameToRestore;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public boolean isDisableOngoingTasks() {
        return disableOngoingTasks;
    }

    public void setDisableOngoingTasks(boolean disableOngoingTasks) {
        this.disableOngoingTasks = disableOngoingTasks;
    }

    public boolean isSkipIndexes() {
        return skipIndexes;
    }

    public void setSkipIndexes(boolean skipIndexes) {
        this.skipIndexes = skipIndexes;
    }

    public ShardedRestoreSettings getShardRestoreSettings() {
        return shardRestoreSettings;
    }

    public void setShardRestoreSettings(ShardedRestoreSettings shardRestoreSettings) {
        this.shardRestoreSettings = shardRestoreSettings;
    }

    public BackupEncryptionSettings getBackupEncryptionSettings() {
        return backupEncryptionSettings;
    }

    public void setBackupEncryptionSettings(BackupEncryptionSettings backupEncryptionSettings) {
        this.backupEncryptionSettings = backupEncryptionSettings;
    }
}
