package net.ravendb.client.documents.operations.backups;

public class RestoreBackupConfiguration extends RestoreBackupConfigurationBase {

    private String backupLocation;

    public String getBackupLocation() {
        return backupLocation;
    }

    public void setBackupLocation(String backupLocation) {
        this.backupLocation = backupLocation;
    }

    @Override
    protected RestoreType getType() {
        return RestoreType.LOCAL;
    }
}
