package net.ravendb.client.documents.operations.backups;

/**
 * Defines the settings required for configuring Amazon S3 (or compatible)
 * storage. These settings are shared by periodic backups and remote
 * attachments.
 */
public interface IS3Settings {
    /**
     * Gets or sets the name of the S3 bucket.
     */
    String getBucketName();
    /**
     * Sets the name of the S3 bucket.
     */
    void setBucketName(String bucketName);

    /**
     * Gets the custom server URL for S3-compatible providers.
     */
    String getCustomServerUrl();

    /**
     * Sets the custom server URL for S3-compatible providers.
     * @param customServerUrl
     */
    void setCustomServerUrl(String customServerUrl);

    /**
     * Gets a value indicating whether to force path-style access.
     */
    boolean isForcePathStyle();

    /**
     * Sets a value indicating whether to force path-style access.
     * @param forcePathStyle
     */
    void setForcePathStyle(boolean forcePathStyle);

    /**
     * Gets the S3 storage class.
     */
    S3StorageClass getStorageClass();

    /**
     * Sets the S3 storage class.
     * @param storageClass
     */
    void setStorageClass(S3StorageClass storageClass);

    /**
     * Gets the AWS Access Key ID.
     */
    String getAwsAccessKey();

    /**
     * Sets the AWS Access Key ID.
     * @param awsAccessKey
     */
    void setAwsAccessKey(String awsAccessKey);

    /**
     * Gets the AWS Secret Access Key.
     */
    String getAwsSecretKey();

    /**
     * Sets the AWS Secret Access Key.
     * @param awsSecretKey
     */
    void setAwsSecretKey(String awsSecretKey);

    /**
     * Gets the AWS Session Token.
     */
    String getAwsSessionToken();

    /**
     * Sets the AWS Session Token.
     * @param awsSessionToken
     */
    void setAwsSessionToken(String awsSessionToken);

    /**
     * Gets the AWS Region name.
     */
    String getAwsRegionName();

    /**
     * Sets the AWS Region name.
     * @param awsRegionName
     */
    void setAwsRegionName(String awsRegionName);

    /**
     * Gets the remote folder path.
     */
    String getRemoteFolderName();

    /**
     * Sets the remote folder path.
     * @param remoteFolderName
     */
    void setRemoteFolderName(String remoteFolderName);

    /**
     * Gets a value indicating whether to disable checksum validation.
     */
    boolean isDisableChecksumValidation();

    /**
     * Sets a value indicating whether to disable checksum validation.
     * @param disableChecksumValidation disable checksum validation
     */
    void setDisableChecksumValidation(boolean disableChecksumValidation);
}
