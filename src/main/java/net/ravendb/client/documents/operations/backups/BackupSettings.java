package net.ravendb.client.documents.operations.backups;

public abstract class BackupSettings {
    private boolean disabled;
    private GetBackupConfigurationScript getBackupConfigurationScript;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public GetBackupConfigurationScript getGetBackupConfigurationScript() {
        return getBackupConfigurationScript;
    }

    public void setGetBackupConfigurationScript(GetBackupConfigurationScript getBackupConfigurationScript) {
        this.getBackupConfigurationScript = getBackupConfigurationScript;
    }
}
