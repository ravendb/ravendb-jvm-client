package net.ravendb.client.documents.operations.etl.olap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.backups.*;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private static final String DESTINATION_FORMAT = "%s-destination@%s";

    @Override
    protected void validateImpl(List<String> errors) {

        if (s3Settings != null) {
            if (!s3Settings.hasSettings()) {
                errors.add("S3Settings has no valid setting. 'BucketName' and 'GetBackupConfigurationScript' are both null");
            }
        }

        if (azureSettings != null) {
            if (!azureSettings.hasSettings()) {
                errors.add("AzureSettings has no valid setting. 'StorageContainer' and 'GetBackupConfigurationScript' are both null");
            }
        }

        if (glacierSettings != null) {
            if (!glacierSettings.hasSettings()) {
                errors.add("GlacierSettings has no valid setting. 'VaultName' and 'GetBackupConfigurationScript' are both null");
            }
        }

        if (googleCloudSettings != null) {
            if (!googleCloudSettings.hasSettings()) {
                errors.add("GoogleCloudSettings has no valid setting. 'BucketName' and 'GetBackupConfigurationScript' are both null");
            }
        }

        if (ftpSettings != null) {
            if (!ftpSettings.hasSettings()) {
                errors.add("FtpSettings has no valid setting. 'Url' is null");
            }
        }

        if (localSettings != null) {
            if (!localSettings.hasSettings()) {
                errors.add("LocalSettings has no valid setting. 'FolderPath' and 'GetBackupConfigurationScript' are both null");
            }
        }
    }

    String getDestination() {
        StringBuilder sb = new StringBuilder();
        String type;
        String destination;

        if (s3Settings != null) {
            type = BackupDestination.AMAZON_S3.getDescription();
            destination = s3Settings.getBucketName();
            if (s3Settings.getRemoteFolderName() != null && !s3Settings.getRemoteFolderName().isEmpty()) {
                destination = destination + "/" + s3Settings.getRemoteFolderName();
            }
            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        if (azureSettings != null) {
            if (sb.length() > 0)
                sb.append(',');

            type = BackupDestination.AZURE.getDescription();
            destination = azureSettings.getStorageContainer();
            if (azureSettings.getRemoteFolderName() != null && !azureSettings.getRemoteFolderName().isEmpty()) {
                destination = destination + "/" + azureSettings.getRemoteFolderName();
            }
            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        if (glacierSettings != null) {
            if (sb.length() > 0)
                sb.append(',');

            type = BackupDestination.AMAZON_GLACIER.getDescription();
            destination = glacierSettings.getVaultName();
            if (glacierSettings.getRemoteFolderName() != null && !glacierSettings.getRemoteFolderName().isEmpty()) {
                destination = destination + "/" + glacierSettings.getRemoteFolderName();
            }
            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        if (googleCloudSettings != null) {
            if (sb.length() > 0)
                sb.append(',');

            type = BackupDestination.GOOGLE_CLOUD.getDescription();
            destination = googleCloudSettings.getBucketName();
            if (googleCloudSettings.getRemoteFolderName() != null && !googleCloudSettings.getRemoteFolderName().isEmpty()) {
                destination = destination + "/" + googleCloudSettings.getRemoteFolderName();
            }
            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        if (ftpSettings != null) {
            if (sb.length() > 0)
                sb.append(',');

            type = BackupDestination.FTP.name();
            destination = ftpSettings.getUrl();
            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        if (localSettings != null) {
            if (sb.length() > 0)
                sb.append(',');

            type = BackupDestination.LOCAL.getDescription();
            destination = localSettings.getFolderPath() != null
                    ? localSettings.getFolderPath()
                    : "CoreDirectory";

            sb.append(String.format(DESTINATION_FORMAT, type, destination));
        }

        return sb.toString();
    }

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
