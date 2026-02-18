package net.ravendb.client.documents.attachments;

/**
 * Defines the contract for remote attachment storage settings.
 *
 * <p>
 * This interface provides a common abstraction for remote attachment storage
 * configuration across different cloud storage providers. It defines the basic
 * property required by all remote attachment storage implementations to
 * organize attachments within the remote storage.
 * </p>
 */
public interface IRemoteAttachmentsSettings {

    /**
     * Gets or sets the remote folder name (prefix) where attachments will be stored.
     * The folder name serves as a logical organization structure within the remote storage,
     * allowing attachments from different databases, environments, or tenants to be stored
     * in separate locations within the same storage account or bucket.
     * The actual interpretation of this folder name depends on the specific storage provider
     * (e.g., S3 key prefix, Azure Blob virtual folder path).
     *
     * @return the folder name or path prefix for storing attachments, or null to use the root.
     */
    String getRemoteFolderName();

    void setRemoteFolderName(String remoteFolderName);
}
