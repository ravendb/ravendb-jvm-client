package net.ravendb.client.documents.operations.backups;

public interface ICloudBackupSettings {
    String getRemoteFolderName();
    void setRemoteFolderName(String remoteFolderName);
}

