package net.ravendb.client.documents.operations.backups;

public class LocalBackup extends BackupStatus {
    private String backupDirectory;
    private String fileName;
    private boolean tempFolderUsed;

    public String getBackupDirectory() {
        return backupDirectory;
    }

    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isTempFolderUsed() {
        return tempFolderUsed;
    }

    public void setTempFolderUsed(boolean tempFolderUsed) {
        this.tempFolderUsed = tempFolderUsed;
    }
}
