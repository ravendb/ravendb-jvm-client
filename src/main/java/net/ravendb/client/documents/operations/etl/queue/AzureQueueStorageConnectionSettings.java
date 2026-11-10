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
