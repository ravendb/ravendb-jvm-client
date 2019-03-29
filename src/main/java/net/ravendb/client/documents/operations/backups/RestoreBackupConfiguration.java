package net.ravendb.client.documents.operations.backups;

public class RestoreBackupConfiguration {
    private String databaseName;
    private String backupLocation;
    private String lastFileNameToRestore;
    private String dataDirectory;
    private String encryptionKey;
    private boolean disableOngoingTasks;
    private boolean skipIndexes;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
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
}
