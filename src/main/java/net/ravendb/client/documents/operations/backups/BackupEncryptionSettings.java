package net.ravendb.client.documents.operations.backups;

public class BackupEncryptionSettings {
    private String key;
    private EncryptionMode encryptionMode;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public EncryptionMode getEncryptionMode() {
        return encryptionMode;
    }

    public void setEncryptionMode(EncryptionMode encryptionMode) {
        this.encryptionMode = encryptionMode;
    }
}
