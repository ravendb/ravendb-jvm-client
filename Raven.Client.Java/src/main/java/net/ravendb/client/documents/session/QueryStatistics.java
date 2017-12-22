package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.QueryResult;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Statistics about a raven query.
 * Such as how many records match the query
 */
public class QueryStatistics {

    private boolean isStale;
    private long durationInMs;
    private int totalResults;
    private int skippedResults;
    private Date timestamp;
    private String indexName;
    private Date indexTimestamp;
    private Date lastQueryTime;
    private Map<String, Double> timingsInMs;
    private Long resultEtag;
    private long resultSize;
    private Map<String, String> scoreExplanations;

    public QueryStatistics() {
        timingsInMs = new HashMap<>();
    }

    /**
     * Whether the query returned potentially stale results
     * @return true is query result is stale
     */
    public boolean isStale() {
        return isStale;
    }

    /**
     * Whether the query returned potentially stale results
     * @param stale sets the value
     */
    public void setStale(boolean stale) {
        isStale = stale;
    }

    /**
     * The duration of the query _server side_
     * @return duration of the query
     */
    public long getDurationInMs() {
        return durationInMs;
    }

    /**
     * The duration of the query _server side_
     * @param durationInMs Sets the value
     */
    public void setDurationInMs(long durationInMs) {
        this.durationInMs = durationInMs;
    }

    /**
     * What was the total count of the results that matched the query
     * @return total results
     */
    public int getTotalResults() {
        return totalResults;
    }

    /**
     * What was the total count of the results that matched the query
     * @param totalResults Sets the value
     */
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    /**
     * Gets the skipped results
     * @return amount of skipped results
     */
    public int getSkippedResults() {
        return skippedResults;
    }

    /**
     * Sets the skipped results
     * @param skippedResults Sets the value
     */
    public void setSkippedResults(int skippedResults) {
        this.skippedResults = skippedResults;
    }

    /**
     * The time when the query results were non stale.
     * @return Query timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * The time when the query results were non stale.
     * @param timestamp Sets the value
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * The name of the index queried
     * @return index name used for query
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The name of the index queried
     * @param indexName Sets the value
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * The timestamp of the queried index
     * @return the index timestamp
     */
    public Date getIndexTimestamp() {
        return indexTimestamp;
    }

    /**
     * The timestamp of the queried index
     * @param indexTimestamp Sets the value
     */
    public void setIndexTimestamp(Date indexTimestamp) {
        this.indexTimestamp = indexTimestamp;
    }

    /**
     * The timestamp of the last time the index was queried
     * @return last query time
     */
    public Date getLastQueryTime() {
        return lastQueryTime;
    }

    /**
     * The timestamp of the last time the index was queried
     * @param lastQueryTime Sets the query time
     */
    public void setLastQueryTime(Date lastQueryTime) {
        this.lastQueryTime = lastQueryTime;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results)
     * @return query timings in milliseconds
     */
    public Map<String, Double> getTimingsInMs() {
        return timingsInMs;
    }

    /**
     * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results)
     * @param timingsInMs Sets the value
     */
    public void setTimingsInMs(Map<String, Double> timingsInMs) {
        this.timingsInMs = timingsInMs;
    }

    public Long getResultEtag() {
        return resultEtag;
    }

    public void setResultEtag(Long resultEtag) {
        this.resultEtag = resultEtag;
    }

    /**
     * The size of the request which were sent from the server.
     * @return result size
     */
    public long getResultSize() {
        return resultSize;
    }

    /**
     * The size of the request which were sent from the server.
     * @param resultSize Sets the value
     */
    public void setResultSize(long resultSize) {
        this.resultSize = resultSize;
    }

    public void updateQueryStats(QueryResult qr) {
        isStale = qr.isStale();
        durationInMs = qr.getDurationInMs();
        totalResults = qr.getTotalResults();
        skippedResults = qr.getSkippedResults();
        timestamp = qr.getIndexTimestamp();
        indexName = qr.getIndexName();
        indexTimestamp = qr.getIndexTimestamp();
        timingsInMs = qr.getTimingsInMs();
        lastQueryTime = qr.getLastQueryTime();
        resultSize = qr.getResultSize();
        resultEtag = qr.getResultEtag();
        scoreExplanations = qr.getScoreExplanations();
    }

    public Map<String, String> getScoreExplanations() {
        return scoreExplanations;
    }

    public void setScoreExplanations(Map<String, String> scoreExplanations) {
        this.scoreExplanations = scoreExplanations;
    }
}
