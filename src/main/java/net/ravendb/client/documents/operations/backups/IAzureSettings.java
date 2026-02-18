package net.ravendb.client.documents.operations.backups;

/**
 * Defines the settings required for Azure Blob Storage configuration.
 * Shared by periodic backups and remote attachments.
 */
public interface IAzureSettings {

    /**
     * Gets or sets the name of the Azure storage container.
     */
    String getStorageContainer();
    void setStorageContainer(String storageContainer);

    /**
     * Gets or sets the remote folder path.
     */
    String getRemoteFolderName();
    void setRemoteFolderName(String remoteFolderName);

    /**
     * Gets or sets the Azure storage account name.
     */
    String getAccountName();
    void setAccountName(String accountName);

    /**
     * Gets or sets the Azure account key.
     */
    String getAccountKey();
    void setAccountKey(String accountKey);

    /**
     * Gets or sets the Shared Access Signature (SAS) token.
     */
    String getSasToken();
    void setSasToken(String sasToken);
}
