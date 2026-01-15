package net.ravendb.client.util;

public class TupleDatabaseServerInfo {
    private final String database;
    private final String server;

    public TupleDatabaseServerInfo(String database, String server) {
        this.database = database;
        this.server = server;
    }

    public String getDatabase() {
        return database;
    }

    public String getServer() {
        return server;
    }
}
