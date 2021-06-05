package net.ravendb.client.documents.operations.revisions;

import java.util.List;

public class RevisionsResult<T> {
    private List<T> results;
    private int totalResults;

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
}
