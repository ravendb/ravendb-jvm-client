package net.ravendb.client.documents.operations.etl.queue;

import net.ravendb.client.documents.operations.etl.sql.SqlConnectionStringParser;

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

    public boolean isValidConnection() {
        if (!isOnlyOneConnectionProvided()) {
            return false;
        }

        if (entraId != null && !entraId.isValid()) {
            return false;
        }

        if (passwordless != null && !passwordless.isValid()) {
            return false;
        }

        return true;
    }

    private boolean isOnlyOneConnectionProvided() {
        int count = 0;

        if (entraId != null)
            count++;

        if (connectionString != null && !connectionString.trim().isEmpty())
            count++;

        if (passwordless != null)
            count++;

        return count == 1;
    }

    public String getStorageUrl() {
        if (connectionString != null) {
            return getUrlFromConnectionString(connectionString);
        }

        String storageAccountName = getStorageAccountName();
        return "https://" + storageAccountName + ".queue.core.windows.net/";
    }

    private String getUrlFromConnectionString(String connectionString) {
        String protocol = SqlConnectionStringParser.getConnectionStringValue(
                connectionString,
                new String[]{"DefaultEndpointsProtocol"}
        );

        if (protocol == null || protocol.trim().isEmpty()) {
            throwConnectionStringError("Protocol not found in the connection string");
        }

        if (protocol.equalsIgnoreCase("http")) {
            String queueEndpoint = SqlConnectionStringParser.getConnectionStringValue(
                    connectionString,
                    new String[]{"QueueEndpoint"}
            );

            if (queueEndpoint == null || queueEndpoint.trim().isEmpty()) {
                throwConnectionStringError("Queue endpoint not found in the connection string");
            }

            return queueEndpoint;
        }

        String accountName = SqlConnectionStringParser.getConnectionStringValue(
                connectionString,
                new String[]{"AccountName"}
        );

        if (accountName == null || accountName.trim().isEmpty()) {
            throwConnectionStringError("Storage account name not found in the connection string");
        }

        return "https://" + accountName + ".queue.core.windows.net/";
    }

    private String getStorageAccountName() {
        if (entraId != null) {
            return entraId.getStorageAccountName();
        } else if (passwordless != null) {
            return passwordless.getStorageAccountName();
        }

        return "";
    }

    private void throwConnectionStringError(String message) {
        throw new IllegalArgumentException(message + " (ConnectionString)");
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

    public boolean isValid() {
        return isNotBlank(storageAccountName)
                && isNotBlank(tenantId)
                && isNotBlank(clientId)
                && isNotBlank(clientSecret);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
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

    public boolean isValid() {
        return storageAccountName != null && !storageAccountName.trim().isEmpty();
    }
}
