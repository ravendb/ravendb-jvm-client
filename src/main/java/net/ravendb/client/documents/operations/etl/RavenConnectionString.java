package net.ravendb.client.documents.operations.etl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    protected void validateImpl(List<String> errors) {

        if (database == null || database.isEmpty()) {
            errors.add("Database cannot be empty");
        }

        if (topologyDiscoveryUrls == null || topologyDiscoveryUrls.length == 0) {
            errors.add("TopologyDiscoveryUrls cannot be empty");
        }

        if (topologyDiscoveryUrls == null) {
            return;
        }

        for (int i = 0; i < topologyDiscoveryUrls.length; i++) {

            if (topologyDiscoveryUrls[i] == null) {
                errors.add("Url number " + (i + 1) + " in TopologyDiscoveryUrls cannot be empty");
                continue;
            }

            topologyDiscoveryUrls[i] = topologyDiscoveryUrls[i].trim();
        }
    }

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.RAVEN;
    }
}
