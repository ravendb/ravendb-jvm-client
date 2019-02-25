package net.ravendb.client.documents.operations.backups;

public class LocalSettings extends BackupSettings {
    private String folderPath;

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
}
