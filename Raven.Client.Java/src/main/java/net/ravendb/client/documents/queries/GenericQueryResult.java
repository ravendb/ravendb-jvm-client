package net.ravendb.client.documents.queries;

import java.util.List;
import java.util.Map;

public class GenericQueryResult<TResult, TIncludes> extends QueryResultBase<TResult, TIncludes> {
    private int totalResults;
    private int skippedResults;
    private Map<String, Map<String, List<String>>> highlightings;
    private long durationInMs;
    private Map<String, String> scoreExplanations;
    private Map<String, Double> timingsInMs;
    private long resultSize;

    /**
     * Gets the total results for this query
     */
    public int getTotalResults() {
        return totalResults;
    }

    /**
     * Sets the total results for this query
     */
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    /**
     * Gets the skipped results
     */
    public int getSkippedResults() {
        return skippedResults;
    }

    /**
     * Sets the skipped results
     */
    public void setSkippedResults(int skippedResults) {
        this.skippedResults = skippedResults;
    }

    /**
     * Highlighter results (if requested).
     */
    public Map<String, Map<String, List<String>>> getHighlightings() {
        return highlightings;
    }

    /**
     * Highlighter results (if requested).
     */
    public void setHighlightings(Map<String, Map<String, List<String>>> highlightings) {
        this.highlightings = highlightings;
    }

    /**
     * The duration of actually executing the query server side
     */
    public long getDurationInMs() {
        return durationInMs;
    }

    /**
     * The duration of actually executing the query server side
     */
    public void setDurationInMs(long durationInMs) {
        this.durationInMs = durationInMs;
    }

    /**
     * Explanations of document scores (if requested).
     */
    public Map<String, String> getScoreExplanations() {
        return scoreExplanations;
    }

    /**
     * Explanations of document scores (if requested).
     */
    public void setScoreExplanations(Map<String, String> scoreExplanations) {
        this.scoreExplanations = scoreExplanations;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
     */
    public Map<String, Double> getTimingsInMs() {
        return timingsInMs;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
     */
    public void setTimingsInMs(Map<String, Double> timingsInMs) {
        this.timingsInMs = timingsInMs;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     */
    public long getResultSize() {
        return resultSize;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     */
    public void setResultSize(long resultSize) {
        this.resultSize = resultSize;
    }

}
