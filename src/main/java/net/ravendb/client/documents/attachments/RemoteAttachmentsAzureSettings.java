package net.ravendb.client.documents.attachments;

import net.ravendb.client.documents.operations.backups.IAzureSettings;

/**
 * Configuration settings for storing attachments in Azure Blob Storage as part
 * of the remote attachments feature.
 *
 * <p>
 * This class defines the remote storage destination for attachments using
 * Microsoft Azure Blob Storage. Remote attachments allow large attachment files
 * to be offloaded from the local RavenDB database to cloud storage, helping
 * reduce database size and improve performance in scenarios involving many or
 * large attachments.
 * </p>
 *
 * <p>
 * Azure authentication can be configured using either of the following:
 * </p>
 *
 * <ul>
 *   <li>
 *     <strong>Account Key</strong>: using {@link #getAccountName()} and
 *     {@link #getAccountKey()}
 *   </li>
 *   <li>
 *     <strong>Shared Access Signature (SAS)</strong>: using
 *     {@link #getAccountName()} and {@link #getSasToken()}
 *   </li>
 * </ul>
 *
 * <p>
 * This class is typically used together with
 * {@link RemoteAttachmentsDestinationConfiguration} to define where and how
 * attachments should be uploaded to Azure Blob Storage.
 * </p>
 */
public final class RemoteAttachmentsAzureSettings implements IRemoteAttachmentsSettings, IAzureSettings {

    /**
     * Gets or sets the name of the Azure Blob Storage container where attachments
     * will be stored.
     *
     * <p>
     * The storage container name must follow Azure Blob Storage naming rules:
     * </p>
     *
     * <ul>
     *   <li>Must be between 3 and 63 characters long</li>
     *   <li>Must contain only lowercase letters, numbers, and dashes</li>
     *   <li>Must start and end with a letter or number</li>
     *   <li>Cannot contain consecutive dashes</li>
     * </ul>
     *
     * <p>
     * This property is required for the configuration to be valid and determines
     * the primary location where attachments will be stored within the Azure
     * storage account.
     * </p>
     */
    private String storageContainer;

    /**
     * Gets or sets the optional subfolder path within the storage container
     * for organizing attachments.
     *
     * <p>
     * This property allows you to organize attachments into a specific folder
     * structure within the Azure container. For example, you might use different
     * folder names for different databases, environments, or tenants.
     * </p>
     *
     * <p>
     * The folder path may include forward slashes to create a multi‑level
     * directory structure. If not specified, attachments will be stored in the
     * root of the container.
     * </p>
     * <p>
     * Example values: {@code "production/database1"} or {@code "attachments/2024"}
     * </p>
     */
    private String remoteFolderName;

    /**
     * Gets or sets the Azure storage account name.
     *
     * <p>
     * This is the name of your Azure storage account and is required for
     * authentication. The account name is used together with either
     * {@link #getAccountKey()} or {@link #getSasToken()} to authenticate with
     * Azure Blob Storage.
     * </p>
     *
     * <p>
     * The storage account must have the appropriate permissions to create and
     * write blobs to the specified container.
     * </p>
     */
    private String accountName;

    /**
     * Gets or sets the Azure storage account key used for authentication.
     *
     * <p>
     * The account key provides full access to the storage account. Use this property
     * when authenticating with the primary or secondary access key of your Azure
     * storage account.
     * </p>
     *
     * <p>
     * You should use either {@link #getAccountKey()} or {@link #getSasToken()} for
     * authentication, but not both. Account keys provide broader access than SAS
     * tokens, so consider using a SAS token with limited permissions if that better
     * fits your security requirements.
     * </p>
     */
    private String accountKey;

    /**
     * Gets or sets the Shared Access Signature (SAS) token used for limited‑scope
     * authentication.
     *
     * <p>
     * A SAS token provides delegated access to resources in your Azure storage
     * account with granular control over permissions, expiration time, and allowed
     * IP addresses. This is often preferred over using an account key for security
     * reasons.
     * </p>
     *
     * <p>
     * The SAS token should include at least the following permissions for the
     * specified container:
     * </p>
     *
     * <ul>
     *   <li><strong>Write (w)</strong>: to upload attachments</li>
     *   <li><strong>Read (r)</strong>: to download attachments when needed</li>
     *   <li><strong>List (l)</strong>: to enumerate attachments if required</li>
     * </ul>
     *
     * <p>
     * You should use either {@link #getSasToken()} or {@link #getAccountKey()} for
     * authentication, but not both.
     * </p>
     */
    private String sasToken;

    /**
     * @return the name of the Azure storage container
     */
    public String getStorageContainer() {
        return storageContainer;
    }

    public void setStorageContainer(String storageContainer) {
        this.storageContainer = storageContainer;
    }

    /**
     * @return the remote folder name, or {@code null} to use the container root
     */
    public String getRemoteFolderName() {
        return remoteFolderName;
    }

    public void setRemoteFolderName(String remoteFolderName) {
        this.remoteFolderName = remoteFolderName;
    }
    /**
     * @return the Azure storage account name
     */
    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
    /**
     * @return the Azure storage account key, or {@code null} if using a SAS token
     */
    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    /**
     * @return the SAS token, or {@code null} if using an account key
     */
    public String getSasToken() {
        return sasToken;
    }

    public void setSasToken(String sasToken) {
        this.sasToken = sasToken;
    }

    boolean hasSettings() {
        return storageContainer != null && !storageContainer.trim().isEmpty();
    }

    boolean isConfigured() {
        if (!hasSettings()) {
            return false;
        }
        return true;
    }
}