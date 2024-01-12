package net.ravendb.client.documents.queries;

import java.util.Map;

public class GenericQueryResult<TResult, TIncludes> extends QueryResultBase<TResult, TIncludes> {
    private long totalResults;
    private Long cappedMaxResults;
    private long skippedResults;
    private Long scannedResults;
    private Map<String, Map<String, String[]>> highlightings;
    private Map<String, String[]> explanations;
    private long durationInMs;
    private long resultSize;

    /**
     * Gets the total results for this query
     * @return Total results for given query
     */
    public long getTotalResults() {
        return totalResults;
    }

    /**
     * Sets the total results for this query
     * @param totalResults Sets the total results
     */
    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }

    /**
     * Gets the total results for the query, taking into account the
     * offset / limit clauses for this query
     * @return Total results
     */
    public Long getCappedMaxResults() {
        return cappedMaxResults;
    }

    /**
     * Sets the total results for the query, taking into account the
     * offset / limit clauses for this query
     * @param cappedMaxResults total results
     */
    public void setCappedMaxResults(Long cappedMaxResults) {
        this.cappedMaxResults = cappedMaxResults;
    }

    /**
     * Gets the skipped results
     * @return Amount of skipped results
     */
    public long getSkippedResults() {
        return skippedResults;
    }

    /**
     * Sets the skipped results
     * @param skippedResults Sets the skipped results
     */
    public void setSkippedResults(long skippedResults) {
        this.skippedResults = skippedResults;
    }

    /**
     * The number of results (filtered or matches)
     * that were scanned by the query. This is relevant
     * only if you are using a filter clause in the query.
     * @return scanned results
     */
    public Long getScannedResults() {
        return scannedResults;
    }

    /**
     * The number of results (filtered or matches)
     * that were scanned by the query. This is relevant
     * only if you are using a filter clause in the query.
     * @param scannedResults scanned results
     */
    public void setScannedResults(Long scannedResults) {
        this.scannedResults = scannedResults;
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
