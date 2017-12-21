package net.ravendb.client.serverwide.etl;

import net.ravendb.client.serverwide.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

public class RavenConnectionString extends ConnectionString {
    private String database;
    private String[] topologyDiscoveryUrls;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String[] getTopologyDiscoveryUrls() {
        return topologyDiscoveryUrls;
    }

    public void setTopologyDiscoveryUrls(String[] topologyDiscoveryUrls) {
        this.topologyDiscoveryUrls = topologyDiscoveryUrls;
    }

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.RAVEN;
    }
}
