package net.ravendb.client.documents.queries;

import java.util.Map;

public class GenericQueryResult<TResult, TIncludes> extends QueryResultBase<TResult, TIncludes> {
    private int totalResults;
    private int skippedResults;
    //TBD private Map<String, Map<String, List<String>>> highlightings;
    private long durationInMs;
    private Map<String, String> scoreExplanations;
    private Map<String, Double> timingsInMs;
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

    //TBD public Map<String, Map<String, List<String>>> getHighlightings()
    //TBD public void setHighlightings(Map<String, Map<String, List<String>>> highlightings) {

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
     * Explanations of document scores (if requested).
     * @return Query score explanations
     */
    public Map<String, String> getScoreExplanations() {
        return scoreExplanations;
    }

    /**
     * Explanations of document scores (if requested).
     * @param scoreExplanations Sets the score explanations
     */
    public void setScoreExplanations(Map<String, String> scoreExplanations) {
        this.scoreExplanations = scoreExplanations;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
     * @return Query timings in milliseconds
     */
    public Map<String, Double> getTimingsInMs() {
        return timingsInMs;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
     * @param timingsInMs Sets the query timings
     */
    public void setTimingsInMs(Map<String, Double> timingsInMs) {
        this.timingsInMs = timingsInMs;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     * @return uncompressed result size
     */
    public long getResultSize() {
        return resultSize;
    }

    /**
     * The size of the request which were sent from the server.
     * This value is the _uncompressed_ size.
     * @param resultSize Sets the result size
     */
    public void setResultSize(long resultSize) {
        this.resultSize = resultSize;
    }

}
