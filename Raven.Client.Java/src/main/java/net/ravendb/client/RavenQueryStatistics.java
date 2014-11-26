package net.ravendb.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.QueryResult;


/**
 * Statistics about a raven query.
 * Such as how many records match the query
 */
public class RavenQueryStatistics {

  private boolean stale;
  private long durationMiliseconds;
  private int totalResults;
  private int skippedResults;
  private Date timestamp;
  private String indexName;
  private Date indexTimestamp;
  private Etag indexEtag;
  private boolean nonAuthoritativeInformation;
  private Date lastQueryTime;
  private Map<String, Double> timingsInMilliseconds;
  private long resultSize;
  private Map<String, String> scoreExplanations;

  /**
   * The size of the request which were sent from the server.
   * This value is the _uncompressed_ size.
   * @return result size
   */
  public long getResultSize() {
    return resultSize;
  }

  public void setResultSize(long resultSize) {
    this.resultSize = resultSize;
  }

  public RavenQueryStatistics() {
    timingsInMilliseconds = new HashMap<>();
  }


  public Map<String, Double> getTimingsInMilliseconds() {
    return timingsInMilliseconds;
  }


  public void setTimingsInMilliseconds(Map<String, Double> timingsInMilliseconds) {
    this.timingsInMilliseconds = timingsInMilliseconds;
  }

  /**
   * @return Whatever the query returned potentially stale results
   */
  public boolean isStale() {
    return stale;
  }

  /**
   * Whatever the query returned potentially stale results
   * @param stale
   */
  public void setStale(boolean stale) {
    this.stale = stale;
  }

  /**
   * @return The duration of the query _server side_
   */
  public long getDurationMiliseconds() {
    return durationMiliseconds;
  }

  /**
   * The duration of the query _server side_
   * @param durationMiliseconds
   */
  public void setDurationMiliseconds(long durationMiliseconds) {
    this.durationMiliseconds = durationMiliseconds;
  }

  /**
   *
   * @return What was the total count of the results that matched the query
   */
  public int getTotalResults() {
    return totalResults;
  }

  /**
   * What was the total count of the results that matched the query
   * @param totalResults
   */
  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
  }

  /**
   * @return the skipped results
   */
  public int getSkippedResults() {
    return skippedResults;
  }

  /**
   * Sets the skipped results
   * @param skippedResults
   */
  public void setSkippedResults(int skippedResults) {
    this.skippedResults = skippedResults;
  }


  /**
   * @return The time when the query results were unstale.
   */
  public Date getTimestamp() {
    return timestamp;
  }

  /**
   * The time when the query results were unstale.
   * @param timestamp
   */
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * @return The name of the index queried
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * The name of the index queried
   * @param indexName
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * @return The timestamp of the queried index
   */
  public Date getIndexTimestamp() {
    return indexTimestamp;
  }

  /**
   *  The timestamp of the queried index
   * @param indexTimestamp
   */
  public void setIndexTimestamp(Date indexTimestamp) {
    this.indexTimestamp = indexTimestamp;
  }

  /**
   * @return The etag of the queried index
   */
  public Etag getIndexEtag() {
    return indexEtag;
  }

  /**
   * The etag of the queried index
   * @param indexEtag
   */
  public void setIndexEtag(Etag indexEtag) {
    this.indexEtag = indexEtag;
  }

  /**
   * Gets a value indicating whether any of the documents returned by this query
   * are non authoritative (modified by uncommitted transaction).
   */
  public boolean isNonAuthoritativeInformation() {
    return nonAuthoritativeInformation;
  }

  /**
   * Sets a value indicating whether any of the documents returned by this query
   * are non authoritative (modified by uncommitted transaction).
   */
  public void setNonAuthoritativeInformation(boolean nonAuthoritativeInformation) {
    this.nonAuthoritativeInformation = nonAuthoritativeInformation;
  }

  /**
   * @return The timestamp of the last time the index was queried
   */
  public Date getLastQueryTime() {
    return lastQueryTime;
  }

  /**
   * The timestamp of the last time the index was queried
   * @param lastQueryTime
   */
  public void setLastQueryTime(Date lastQueryTime) {
    this.lastQueryTime = lastQueryTime;
  }

  public void updateQueryStats(QueryResult qr) {
    stale = qr.isStale();
    durationMiliseconds = qr.getDurationMiliseconds();
    nonAuthoritativeInformation = qr.isNonAuthoritativeInformation();
    totalResults = qr.getTotalResults();
    skippedResults = qr.getSkippedResults();
    timestamp = qr.getIndexTimestamp();
    indexName = qr.getIndexName();
    indexTimestamp = qr.getIndexTimestamp();
    lastQueryTime = qr.getLastQueryTime();
    timingsInMilliseconds = qr.getTimingsInMilliseconds();
    indexEtag = qr.getIndexEtag();
    resultSize = qr.getResultSize();
    scoreExplanations = qr.getScoreExplanations();
  }

  public Map<String, String> getScoreExplanations() {
    return scoreExplanations;
  }

}
