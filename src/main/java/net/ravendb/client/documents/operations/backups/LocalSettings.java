package net.ravendb.client.documents.operations.backups;

public class LocalSettings extends BackupSettings {
    private String folderPath;
    private Integer shardNumber;

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Integer getShardNumber() {
        return shardNumber;
    }

    public void setShardNumber(Integer shardNumber) {
        this.shardNumber = shardNumber;
    }
}
