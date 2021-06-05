package net.ravendb.client.documents.operations.replication;

import java.util.List;

public class ReplicationHubAccessResult {
    private List<DetailedReplicationHubAccess> results;

    public List<DetailedReplicationHubAccess> getResults() {
        return results;
    }

    public void setResults(List<DetailedReplicationHubAccess> results) {
        this.results = results;
    }
}
