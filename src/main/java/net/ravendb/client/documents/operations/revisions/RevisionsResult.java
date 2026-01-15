package net.ravendb.client.documents.operations.revisions;

import java.util.List;

/**
 * Represents the result of a {@link GetRevisionsOperation}, containing a list of revision objects and the total count of results.
 *
 * @param <T> The type of the document for which the revisions are being retrieved (contained in the results).
 */
public class RevisionsResult<T> {
    /**
     * The list of revisions.
     */
    private List<T> results;
    /**
     * Total number of revisions the document has.
     */
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
