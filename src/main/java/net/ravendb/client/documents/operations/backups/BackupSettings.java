package net.ravendb.client.documents.operations.backups;

public abstract class BackupSettings {
    private boolean disabled;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
