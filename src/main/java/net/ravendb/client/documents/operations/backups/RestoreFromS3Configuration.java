package net.ravendb.client.documents.operations.backups;

public class RestoreFromS3Configuration extends RestoreBackupConfigurationBase {
    private S3Settings settings;


    @Override
    protected RestoreType getType() {
        return RestoreType.S3;
    }

    public S3Settings getSettings() {
        return settings;
    }

    public void setSettings(S3Settings settings) {
        this.settings = settings;
    }
}
