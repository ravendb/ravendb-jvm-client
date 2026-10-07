package net.ravendb.client.documents.attachments;

import net.ravendb.client.documents.operations.backups.IS3Settings;
import net.ravendb.client.documents.operations.backups.S3StorageClass;

/**
 * Configuration settings for storing remote attachments in Amazon S3 or
 * S3‑compatible storage.
 *
 * <p>
 * This class provides the configuration required to upload and store RavenDB
 * attachments in Amazon S3 or S3‑compatible object storage services. It
 * implements S3‑specific settings as well as the general remote storage
 * configuration model, and supports JSON serialization.
 * </p>
 *
 * <p>
 * The configuration supports authentication using AWS access keys, secret keys,
 * and optional session tokens for temporary credentials. It also allows
 * customization of storage class, server URL, and path‑style access options for
 * compatibility with various S3‑compatible storage providers.
 * </p>
 */
public final class RemoteAttachmentsS3Settings implements IS3Settings, IRemoteAttachmentsSettings {

    /**
     * Gets or sets the AWS access key ID used for authentication with Amazon S3.
     *
     * <p>
     * This is the first part of the AWS credential pair (Access Key ID and Secret
     * Access Key). The access key ID identifies the AWS account or IAM user making
     * requests to S3.
     * </p>
     *
     * <p>
     * For security best practices, consider using IAM roles or temporary
     * credentials via {@link #getAwsSessionToken()} instead of long‑term access
     * keys whenever possible.
     * </p>
     * @see #getAwsSecretKey()
     * @see #getAwsSessionToken()
     */
    private String awsAccessKey;

    /**
     * Gets or sets the AWS secret access key used for authentication with Amazon S3.
     *
     * <p>
     * This is the second part of the AWS credential pair (Access Key ID and Secret
     * Access Key). The secret key is used to sign requests to AWS services and must
     * be kept strictly confidential.
     * </p>
     *
     * <p><strong>Security Warning:</strong>
     * Never commit secret keys to source control or expose them in logs or error
     * messages. Store them securely using environment variables, configuration
     * management systems, or secret‑management services.
     * </p>
     * @see #getAwsAccessKey()
     */
    private String awsSecretKey;

    /**
     * Gets or sets the AWS session token for temporary security credentials.
     *
     * <p>
     * Session tokens are used when working with temporary security credentials
     * obtained from AWS Security Token Service (STS). These are typically used
     * with IAM roles, federated users, or when assuming roles across AWS accounts.
     * </p>
     *
     * <p>
     * Temporary credentials automatically expire after a specified duration,
     * providing enhanced security compared to long‑term access keys. This is the
     * recommended authentication method for production environments.
     * </p>
     * @see #getAwsAccessKey()
     * @see #getAwsSecretKey()
     */
    private String awsSessionToken;

    /**
     * Gets or sets the AWS region name where the S3 bucket is located.
     *
     * <p>
     * The region name specifies the geographic location of the S3 bucket. Common
     * region names include:
     * </p>
     *
     * <ul>
     *   <li>us-east-1 (US East, N. Virginia)</li>
     *   <li>us-west-2 (US West, Oregon)</li>
     *   <li>eu-west-1 (Europe, Ireland)</li>
     *   <li>ap-southeast-1 (Asia Pacific, Singapore)</li>
     * </ul>
     *
     * <p>
     * For S3‑compatible storage providers that do not use AWS regions, this value
     * may be used to specify alternative region identifiers or left empty,
     * depending on the provider's requirements.
     * </p>
     */
    private String awsRegionName;

    /**
     * Gets or sets the remote folder name (prefix) where attachments will be
     * stored within the S3 bucket.
     *
     * <p>
     * This property specifies a folder path or key prefix within the S3 bucket
     * where attachment objects will be stored. It helps organize attachments and
     * can be used to separate different databases, environments, or tenants within
     * the same bucket.
     * </p>
     */
    private String remoteFolderName;

    /**
     * Gets the name of the S3 bucket where attachments will be stored.
     *
     * <p>
     * This is a required property. The bucket must already exist, and the configured
     * credentials must have the necessary permissions to upload, download, and list
     * objects in the bucket.
     * </p>
     *
     * <p>
     * Bucket names must follow AWS S3 naming rules:
     * </p>
     *
     * <ul>
     *   <li>Must be globally unique across all AWS accounts</li>
     *   <li>Must be between 3 and 63 characters long</li>
     *   <li>Must consist only of lowercase letters, numbers, dots, and hyphens</li>
     *   <li>Must begin and end with a letter or number</li>
     * </ul>
     */
    private String bucketName;

    /**
     * Gets or sets a custom server URL for S3‑compatible storage providers.
     *
     * <p>
     * Use this property when connecting to S3‑compatible storage providers other
     * than AWS S3, such as MinIO, Wasabi, DigitalOcean Spaces, or on‑premises
     * S3‑compatible solutions.
     * </p>
     *
     * <p>
     * The URL should include the protocol ({@code http://} or {@code https://})
     * and may include a port number. Examples include:
     * {@code "https://s3.example.com:9000"} or
     * {@code "https://nyc3.digitaloceanspaces.com"}.
     * </p>
     *
     * <p>
     * When using AWS S3, leave this property {@code null} to use the default AWS
     * S3 endpoints determined by {@link #getAwsRegionName()}.
     * </p>
     */
    private String customServerUrl;

    /**
     * Gets or sets a value indicating whether to use path‑style addressing for S3 requests.
     *
     * <p>
     * Path‑style URLs include the bucket name in the URL path:
     * <code>http://s3.amazonaws.com/bucket/key</code>
     * Virtual‑hosted‑style URLs (default) include the bucket name as a subdomain:
     * <code>http://bucket.s3.amazonaws.com/key</code>
     * </p>
     *
     * <p>
     * Set this to {@code true} when:
     * </p>
     *
     * <ul>
     *   <li>Working with S3‑compatible storage that requires path‑style access (e.g., MinIO)</li>
     *   <li>Using buckets with dots in their names that may cause SSL certificate validation issues</li>
     *   <li>Required by specific S3‑compatible storage provider configurations</li>
     * </ul>
     *
     * <p>
     * Note: AWS S3 is deprecating path‑style access, but it remains necessary for many
     * S3‑compatible services.
     * </p>
     *
     * <p>
     * {@code true} to use path‑style addressing; {@code false} to use virtual‑hosted‑style (default).
     * </p>
     *
     * @see #getCustomServerUrl()
     */
    private boolean forcePathStyle;

    /**
     * Gets or sets a value indicating whether to disable checksum validation.
     *
     * <p>
     * This property disables checksum validation for S3 uploads. Checksum
     * validation ensures data integrity and should not be disabled if not
     * necessary.
     * </p>
     *
     * <p>
     * Set this to {@code true} if your S3‑compatible storage does not support
     * modern object integrity checks.
     * </p>
     */
    private boolean disableChecksumValidation;

    /**
     * Gets or sets the S3 storage class to use for stored attachments.
     *
     * <p>
     * The storage class determines the availability, durability, and cost of stored
     * objects. Different storage classes are optimized for different access patterns:
     * </p>
     *
     * <ul>
     *   <li>{@link S3StorageClass#GLACIER} – Low‑cost archival storage with retrieval
     *       times of minutes to hours</li>
     *   <li>{@link S3StorageClass#GLACIER_INSTANT_RETRIEVAL} – Archival storage with
     *       millisecond retrieval</li>
     *   <li>{@link S3StorageClass#DEEP_ARCHIVE} – Lowest‑cost storage for long‑term
     *       archival with retrieval times of up to 12 hours</li>
     * </ul>
     *
     * <p>
     * Set this to {@code null} to use the default storage class (STANDARD). Choose
     * archival storage classes for cost optimization when attachments are accessed
     * infrequently.
     * </p>
     * @see S3StorageClass
     */
    private S3StorageClass storageClass;

    /**
     * @return the AWS access key ID, or {@code null} if using alternative
     *         authentication methods
     */
    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    /**
     * @return the AWS secret access key, or {@code null} if using alternative
     *         authentication methods
     */
    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    /**
     * @return the AWS session token for temporary credentials, or {@code null}
     *         if using long‑term credentials
     */
    public String getAwsSessionToken() {
        return awsSessionToken;
    }

    public void setAwsSessionToken(String awsSessionToken) {
        this.awsSessionToken = awsSessionToken;
    }

    /**
     * @return the AWS region name (e.g., {@code "us-east-1"}), or {@code null}
     *         for default or S3‑compatible services
     */
    public String getAwsRegionName() {
        return awsRegionName;
    }

    public void setAwsRegionName(String awsRegionName) {
        this.awsRegionName = awsRegionName;
    }

    /**
     * @return the folder name or key prefix, or {@code null} to store attachments
     *         at the bucket root
     */
    @Override
    public String getRemoteFolderName() {
        return remoteFolderName;
    }

    @Override
    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }

    /**
     * @return the S3 bucket name, or {@code null} if not configured
     */
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * @return the custom server URL for S3‑compatible storage, or {@code null} to
     *         use AWS S3 default endpoints
     */
    public String getCustomServerUrl() {
        return customServerUrl;
    }

    public void setCustomServerUrl(String customServerUrl) {
        this.customServerUrl = customServerUrl;
    }
    /**
     * @return {@code true} to use path‑style addressing; {@code false} to use virtual‑hosted‑style (default).
     */
    public boolean isForcePathStyle() {
        return forcePathStyle;
    }

    public void setForcePathStyle(boolean forcePathStyle) {
        this.forcePathStyle = forcePathStyle;
    }

    /**
     * @return {@code true} when checksum validation is disabled for S3 uploads; {@code false} (default) to keep it enabled.
     */
    @Override
    public boolean isDisableChecksumValidation() {
        return disableChecksumValidation;
    }

    @Override
    public void setDisableChecksumValidation(boolean disableChecksumValidation) {
        this.disableChecksumValidation = disableChecksumValidation;
    }

     /**
     * @return The S3 storage class for attachments, or {@code null} to use the default STANDARD storage class.
     */
    public S3StorageClass getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(S3StorageClass storageClass) {
        this.storageClass = storageClass;
    }

    /**
     * Minimal enabling condition — bucket must be set.
     */
    boolean isConfigured() {
        return bucketName != null && !bucketName.trim().isEmpty();
    }
}