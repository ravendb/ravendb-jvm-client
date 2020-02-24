package net.ravendb.client.documents.operations.backups;

public class RestoreFromGoogleCloudConfiguration extends RestoreBackupConfigurationBase {
    private GoogleCloudSettings settings;

    @Override
    protected RestoreType getType() {
        return RestoreType.GOOGLE_CLOUD;
    }

    public GoogleCloudSettings getSettings() {
        return settings;
    }

    public void setSettings(GoogleCloudSettings settings) {
        this.settings = settings;
    }
}
