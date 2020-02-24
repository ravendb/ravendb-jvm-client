package net.ravendb.client.documents.operations.backups;

public class RestoreFromAzureConfiguration extends RestoreBackupConfigurationBase {
    private AzureSettings settings;

    @Override
    protected RestoreType getType() {
        return RestoreType.AZURE;
    }

    public AzureSettings getSettings() {
        return settings;
    }

    public void setSettings(AzureSettings settings) {
        this.settings = settings;
    }
}
