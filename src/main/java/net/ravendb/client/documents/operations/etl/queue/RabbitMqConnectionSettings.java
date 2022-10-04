package net.ravendb.client.documents.operations.etl.queue;

public class RabbitMqConnectionSettings {

    private String connectionString;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }
}
