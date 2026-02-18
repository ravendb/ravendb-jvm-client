package net.ravendb.client.documents.attachments;

/**
 * Configuration settings for the destination where remote attachments will be stored.
 *
 * <p>
 * This class defines the cloud storage destination configuration for RavenDB's
 * remote attachments feature. Remote attachments allow large attachment files
 * to be offloaded from the local RavenDB database to external cloud storage
 * providers, reducing database size and improving performance in scenarios
 * involving many or large attachments.
 * </p>
 *
 * <p>
 * The configuration supports two mutually exclusive cloud storage providers:
 * </p>
 *
 * <ul>
 *   <li>Amazon S3 (via {@link #getS3Settings()})</li>
 *   <li>Microsoft Azure Blob Storage (via {@link #getAzureSettings()})</li>
 * </ul>
 *
 * <p>
 * Only one provider can be configured at a time. The configuration will be
 * validated to ensure that exactly one uploader is configured when the feature
 * is enabled.
 * </p>
 */
public final class RemoteAttachmentsDestinationConfiguration {

    /**
     * Gets or sets a value indicating whether remote attachments functionality
     * is disabled for this destination.
     *
     * <p>
     * When set to {@code true}, the remote attachments feature is disabled and
     * attachments will not be uploaded to the configured cloud storage destination.
     * Instead, attachments will be stored locally in the database.
     * </p>
     *
     * <p>
     * When set to {@code false}, attachments will be uploaded to the configured
     * cloud storage provider (either S3 or Azure), according to the settings
     * specified in {@link #getS3Settings()} or {@link #getAzureSettings()}.
     * </p>
     *
     */
    private boolean disabled;

    /**
     * Gets or sets the Amazon S3 storage configuration for remote attachments.
     *
     * <p>
     * This property configures Amazon S3 as the destination for remote attachments.
     * When configured, attachments will be uploaded to the specified S3 bucket
     * using the provided credentials.
     * </p>
     *
     * <p>
     * This setting is mutually exclusive with {@link #getAzureSettings()}.
     * Only one cloud storage provider can be configured at a time. The configuration
     * will be validated to ensure this constraint.
     * </p>
     * @see RemoteAttachmentsS3Settings
     * @see #getAzureSettings()
     */
    private RemoteAttachmentsS3Settings s3Settings;

    /**
     * Gets or sets the Microsoft Azure Blob Storage configuration for remote attachments.
     *
     * <p>
     * This property configures Azure Blob Storage as the destination for remote
     * attachments. When configured, attachments will be uploaded to the specified
     * Azure storage container using the provided credentials.
     * </p>
     *
     * <p>
     * This setting is mutually exclusive with {@link #getS3Settings()}.
     * Only one cloud storage provider can be configured at a time. The configuration
     * will be validated to ensure this constraint.
     * </p>
     * @see RemoteAttachmentsAzureSettings
     * @see #getS3Settings()
     */
    private RemoteAttachmentsAzureSettings azureSettings;

    /**
     * @return {@code true} if remote attachments are disabled; otherwise {@code false}.
     * The default value is {@code false}.
     */
    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return the S3 settings configuration, or {@code null} if Azure is being used instead
     */
    public RemoteAttachmentsS3Settings getS3Settings() {
        return s3Settings;
    }

    public void setS3Settings(RemoteAttachmentsS3Settings s3Settings) {
        this.s3Settings = s3Settings;
    }
    /**
     * @return the Azure Blob Storage settings configuration, or {@code null} if S3 is being used instead
     */
    public RemoteAttachmentsAzureSettings getAzureSettings() {
        return azureSettings;
    }

    public void setAzureSettings(RemoteAttachmentsAzureSettings azureSettings) {
        this.azureSettings = azureSettings;
    }

    /**
     * Validates the configuration for this destination.
     */
    void assertConfiguration(String key, String databaseName) {
        String suffix = (databaseName == null || databaseName.isEmpty())
                ? ""
                : " for database '" + databaseName + "'";

        boolean s3Configured = s3Settings != null && s3Settings.isConfigured();
        boolean azureConfigured = azureSettings != null && azureSettings.isConfigured();

        if (!s3Configured && !azureConfigured) {
            throw new IllegalStateException("Exactly one uploader for RemoteAttachmentsConfiguration" + suffix + " must be configured.");
        }

        if (s3Configured && azureConfigured) {
            throw new IllegalStateException("Only one uploader for RemoteAttachmentsConfiguration" + suffix + " can be configured.");
        }
    }
}
