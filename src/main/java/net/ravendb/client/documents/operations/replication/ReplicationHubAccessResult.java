package net.ravendb.client.documents.operations.replication;

public class ReplicationHubAccessResult {
    private DetailedReplicationHubAccess[] results;


    public DetailedReplicationHubAccess[] getResults() {
        return results;
    }

    public void setResults(DetailedReplicationHubAccess[] results) {
        this.results = results;
    }
}
