package net.ravendb.client.documents.operations.etl.queue;

public final class AzureQueueStorageConnectionSettings {

    private EntraId entraId;
    private String connectionString;
    private Passwordless passwordless;

    public EntraId getEntraId() {
        return entraId;
    }

    public void setEntraId(EntraId entraId) {
        this.entraId = entraId;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public Passwordless getPasswordless() {
        return passwordless;
    }

    public void setPasswordless(Passwordless passwordless) {
        this.passwordless = passwordless;
    }
}

final class EntraId {

    private String storageAccountName;
    private String tenantId;
    private String clientId;
    private String clientSecret;

    public String getStorageAccountName() {
        return storageAccountName;
    }

    public void setStorageAccountName(String storageAccountName) {
        this.storageAccountName = storageAccountName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}

final class Passwordless {

    private String storageAccountName;

    public String getStorageAccountName() {
        return storageAccountName;
    }

    public void setStorageAccountName(String storageAccountName) {
        this.storageAccountName = storageAccountName;
    }
}
