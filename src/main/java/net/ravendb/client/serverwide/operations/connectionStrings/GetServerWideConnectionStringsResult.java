package net.ravendb.client.serverwide.operations.connectionStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of a {@link GetServerWideConnectionStringsOperation}.
 */
public class GetServerWideConnectionStringsResult {

    private List<ServerWideConnectionString> results = new ArrayList<>();

    /**
     * @return the list of server-wide connection strings matching the query criteria
     */
    public List<ServerWideConnectionString> getResults() {
        return results;
    }

    public void setResults(List<ServerWideConnectionString> results) {
        this.results = results;
    }
}
