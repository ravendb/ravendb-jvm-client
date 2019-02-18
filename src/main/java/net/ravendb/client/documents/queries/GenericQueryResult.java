package net.ravendb.client.documents.queries;

import java.util.Map;

public class GenericQueryResult<TResult, TIncludes> extends QueryResultBase<TResult, TIncludes> {
    private int totalResults;
    private int skippedResults;
    private Map<String, Map<String, String[]>> highlightings;
    private Map<String, String[]> explanations;
    private long durationInMs;
    private long resultSize;

    /**
     * Gets the total results for this query
     * @return Total results for given query
     */
    public int getTotalResults() {
        return totalResults;
    }

    /**
     * Sets the total results for this query
     * @param totalResults Sets the total results
     */
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    /**
     * Gets the skipped results
     * @return Amount of skipped results
     */
    public int getSkippedResults() {
        return skippedResults;
    }

    /**
     * Sets the skipped results
     * @param skippedResults Sets the skipped results
     */
    public void setSkippedResults(int skippedResults) {
        this.skippedResults = skippedResults;
    }

    /**
     * @return Highlighter results (if requested).
     */
    public Map<String, Map<String, String[]>> getHighlightings() {
        return highlightings;
    }

    /**
     * @param highlightings Highlighter results (if requested).
     */
    public void setHighlightings(Map<String, Map<String, String[]>> highlightings) {
        this.highlightings = highlightings;
    }

    /**
     * @return Explanations (if requested).
     */
    public Map<String, String[]> getExplanations() {
        return explanations;
    }

    /**
     * @param explanations Explanations (if requested).
     */
    public void setExplanations(Map<String, String[]> explanations) {
        this.explanations = explanations;
    }

    /**
     * The duration of actually executing the query server side
     * @return Query duration in milliseconds
     */
    public long getDurationInMs() {
        return durationInMs;
    }

    /**
     * The duration of actually executing the query server side
     * @param durationInMs Sets the query duration
     */
    public void setDurationInMs(long durationInMs) {
        this.durationInMs = durationInMs;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     * @deprecated ResultSize is not supported anymore. Will be removed in next major version of the product.
     * @return uncompressed result size
     */
    @Deprecated
    public long getResultSize() {
        return resultSize;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     * @deprecated ResultSize is not supported anymore. Will be removed in next major version of the product.
     * @param resultSize Sets the result size
     */
    @Deprecated
    public void setResultSize(long resultSize) {
        this.resultSize = resultSize;
    }

}
