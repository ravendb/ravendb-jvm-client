package net.ravendb.client.documents.changes;

public class TopologyChange extends DatabaseChange {
    private String url;
    private String database;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
