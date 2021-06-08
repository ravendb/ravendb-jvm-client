package net.ravendb.client.documents.operations.etl.olap;

import net.ravendb.client.documents.operations.backups.*;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

public class OlapConnectionString extends ConnectionString {
    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.OLAP;
    }

    private LocalSettings localSettings;
    private S3Settings s3Settings;
    private AzureSettings azureSettings;
    private GlacierSettings glacierSettings;
    private GoogleCloudSettings googleCloudSettings;
    private FtpSettings ftpSettings;

    public LocalSettings getLocalSettings() {
        return localSettings;
    }

    public void setLocalSettings(LocalSettings localSettings) {
        this.localSettings = localSettings;
    }

    public S3Settings getS3Settings() {
        return s3Settings;
    }

    public void setS3Settings(S3Settings s3Settings) {
        this.s3Settings = s3Settings;
    }

    public AzureSettings getAzureSettings() {
        return azureSettings;
    }

    public void setAzureSettings(AzureSettings azureSettings) {
        this.azureSettings = azureSettings;
    }

    public GlacierSettings getGlacierSettings() {
        return glacierSettings;
    }

    public void setGlacierSettings(GlacierSettings glacierSettings) {
        this.glacierSettings = glacierSettings;
    }

    public GoogleCloudSettings getGoogleCloudSettings() {
        return googleCloudSettings;
    }

    public void setGoogleCloudSettings(GoogleCloudSettings googleCloudSettings) {
        this.googleCloudSettings = googleCloudSettings;
    }

    public FtpSettings getFtpSettings() {
        return ftpSettings;
    }

    public void setFtpSettings(FtpSettings ftpSettings) {
        this.ftpSettings = ftpSettings;
    }
}
