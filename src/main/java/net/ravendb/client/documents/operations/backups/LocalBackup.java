package net.ravendb.client.documents.operations.backups;

public class LocalBackup extends BackupStatus {
    private String backupDirectory;
    private boolean tempFolderUsed;

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public boolean isTempFolderUsed() {
        return tempFolderUsed;
    }

    public void setTempFolderUsed(boolean tempFolderUsed) {
        this.tempFolderUsed = tempFolderUsed;
    }
}
