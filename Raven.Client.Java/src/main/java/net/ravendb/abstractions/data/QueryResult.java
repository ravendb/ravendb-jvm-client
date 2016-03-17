package net.ravendb.abstractions.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.json.linq.RavenJObject;


public class QueryResult {
  private List<RavenJObject> results;
  private List<RavenJObject> includes;
  private boolean isStale;
  private Date indexTimestamp;
  private int totalResults;
  private int skippedResults;
  private String indexName;
  private Etag indexEtag;
  private Etag resultEtag;
  private Map<String, Map<String , String[]>> highlightings;
  private boolean nonAuthoritativeInformation;
  private Date lastQueryTime;
  private long durationMiliseconds;
  private Map<String, String> scoreExplanations;
  private Map<String, Double> timingsInMilliseconds;
  private long resultSize;

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
   * @param resultSize
   */
  public void setResultSize(long resultSize) {
    this.resultSize = resultSize;
  }

  /**
   * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
   */
  public Map<String, Double> getTimingsInMilliseconds() {
    return timingsInMilliseconds;
  }

  /**
   * Detailed timings for various parts of a query (Lucene search, loading documents, transforming results) - if requested.
   * @param timingsInMilliseconds
   */
  public void setTimingsInMilliseconds(Map<String, Double> timingsInMilliseconds) {
    this.timingsInMilliseconds = timingsInMilliseconds;
  }

  /**
   * Initializes a new instance of the {@link QueryResult} class.
   */
  public QueryResult() {
    results = new ArrayList<>();
    includes = new ArrayList<>();
  }

  /**
   * Explanations of document scores (if requested).
   */
  public Map<String, String> getScoreExplanations() {
    return scoreExplanations;
  }

  /**
   * Explanations of document scores (if requested).
   * @param scoreExplanations
   */
  public void setScoreExplanations(Map<String, String> scoreExplanations) {
    this.scoreExplanations = scoreExplanations;
  }

  /**
   * Creates a snapshot of the query results
   * @return QueryResult snapshot
   */
  public QueryResult createSnapshot() {
    QueryResult snapshot = new QueryResult();
    for (RavenJObject obj: results) {
      if (obj != null) {
        snapshot.getResults().add(obj.createSnapshot());
      } else {
        snapshot.getResults().add(null);
      }
    }
    for (RavenJObject obj: includes) {
      snapshot.getIncludes().add(obj.createSnapshot());
    }
    snapshot.setIndexEtag(getIndexEtag());
    snapshot.setIndexName(getIndexName());
    snapshot.setIndexTimestamp(getIndexTimestamp());
    snapshot.setStale(isStale);
    snapshot.setSkippedResults(skippedResults);
    snapshot.setTotalResults(getTotalResults());

    if (highlightings != null) {
      snapshot.setHighlightings(new HashMap<String, Map<String,String[]>>());
      for (Map.Entry<String, Map<String, String[]>> entry: highlightings.entrySet()) {
        snapshot.getHighlightings().put(entry.getKey(), new HashMap<>(entry.getValue()));
      }
    } else {
      snapshot.setHighlightings(null);
    }

    if (scoreExplanations != null) {
      snapshot.setScoreExplanations(new HashMap<String, String>());
      for (Map.Entry<String, String> entry: scoreExplanations.entrySet()) {
        snapshot.getScoreExplanations().put(entry.getKey(), entry.getValue());
      }
    } else {
      snapshot.setScoreExplanations(null);
    }

    if (timingsInMilliseconds != null) {
      snapshot.setTimingsInMilliseconds(new HashMap<String, Double>());
      for(Map.Entry<String, Double> entry: timingsInMilliseconds.entrySet()) {
        snapshot.getTimingsInMilliseconds().put(entry.getKey(), entry.getValue());
      }
    } else {
      snapshot.setTimingsInMilliseconds(null);
    }
    snapshot.setLastQueryTime(getLastQueryTime());
    snapshot.setDurationMiliseconds(getDurationMiliseconds());
    snapshot.setNonAuthoritativeInformation(isNonAuthoritativeInformation());
    snapshot.setResultEtag(getResultEtag());
    return snapshot;
  }

  /**
   * Ensures that the query results can be used in snapshots
   */
  public void ensureSnapshot() {
    for(RavenJObject result: results) {
      if (result != null) {
        result.ensureCannotBeChangeAndEnableShapshotting();
      }
    }
    for(RavenJObject include: includes) {
      include.ensureCannotBeChangeAndEnableShapshotting();
    }
  }

  /**
   * The duration of actually executing the query server side
   * -1 is returned in case the query results retrieved from cache
   */
  public long getDurationMiliseconds() {
    return durationMiliseconds;
  }

  /**
   * Highlighter results (if requested).
   */
  public Map<String, Map<String, String[]>> getHighlightings() {
    return highlightings;
  }

  /**
   * Gets the document included in the result.
   */
  public Collection<RavenJObject> getIncludes() {
    return includes;
  }

  /**
   * The last etag indexed by the index.
   * This can be used to determine whatever the results can be cached.
   */
  public Etag getIndexEtag() {
    return indexEtag;
  }

  /**
   * The index used to answer this query
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * The last time the index was updated.
   * This can be used to determine the freshness of the data.
   * @return
   */
  public Date getIndexTimestamp() {
    return indexTimestamp;
  }

  /**
   * The timestamp of the last time the index was queried
   */
  public Date getLastQueryTime() {
    return lastQueryTime;
  }

  /**
   * The ETag value for this index current state, which include what docs were indexed,
   * what document were deleted, etc.
   */
  public Etag getResultEtag() {
    return resultEtag;
  }

  /**
   * Gets the document resulting from this query.
   */
  public List<RavenJObject> getResults() {
    return results;
  }

  /**
   * Gets the skipped results
   */
  public int getSkippedResults() {
    return skippedResults;
  }

  /**
   * Gets the total results for this query
   */
  public int getTotalResults() {
    return totalResults;
  }

  /**
   * Indicates whether any of the documents returned by this query
   * are non authoritative (modified by uncommitted transaction).
   */
  public boolean isNonAuthoritativeInformation() {
    return nonAuthoritativeInformation;
  }

  /**
   * Gets a value indicating whether the index is stale.
   * Value:
   * - true - if index is stale
   * - false - otherwise
   * {@value true if the index is stale; otherwise, false.}
   */
  public boolean isStale() {
    return isStale;
  }

  /**
   * The duration of actually executing the query server side
   * -1 is returned in case the query results retrieved from cache
   * @param durationMiliseconds
   */
  public void setDurationMiliseconds(long durationMiliseconds) {
    this.durationMiliseconds = durationMiliseconds;
  }

  /**
   * Highlighter results (if requested).
   * @param highlightings
   */
  public void setHighlightings(Map<String, Map<String, String[]>> highlightings) {
    this.highlightings = highlightings;
  }

  /**
   * Sets the document included in the result.
   * @param includes
   */
  public void setIncludes(List<RavenJObject> includes) {
    this.includes = includes;
  }

  /**
   * The last etag indexed by the index.
   * This can be used to determine whatever the results can be cached.
   * @param indexEtag
   */
  public void setIndexEtag(Etag indexEtag) {
    this.indexEtag = indexEtag;
  }

  /**
   * The index used to answer this query
   * @param indexName
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * The last time the index was updated.
   * This can be used to determine the freshness of the data.
   * @param indexTimestamp
   */
  public void setIndexTimestamp(Date indexTimestamp) {
    this.indexTimestamp = indexTimestamp;
  }

  /**
   * The timestamp of the last time the index was queried
   * @param lastQueryTime
   */
  public void setLastQueryTime(Date lastQueryTime) {
    this.lastQueryTime = lastQueryTime;
  }

  /**
   * Indicates whether any of the documents returned by this query
   * are non authoritative (modified by uncommitted transaction).
   * @param nonAuthoritativeInformation
   */
  public void setNonAuthoritativeInformation(boolean nonAuthoritativeInformation) {
    this.nonAuthoritativeInformation = nonAuthoritativeInformation;
  }

  /**
   * The ETag value for this index current state, which include what docs were indexed,
   * what document were deleted, etc.
   * @param resultEtag
   */
  public void setResultEtag(Etag resultEtag) {
    this.resultEtag = resultEtag;
  }

  /**
   * Sets the document resulting from this query.
   * @param results
   */
  public void setResults(List<RavenJObject> results) {
    this.results = results;
  }

  /**
   * Sets the skipped results
   * @param skippedResults
   */
  public void setSkippedResults(int skippedResults) {
    this.skippedResults = skippedResults;
  }

  /**
   * Sets a value indicating whether the index is stale.
   * Value:
   * - true - if index is stale
   * - false - otherwise
   * {@value true if the index is stale; otherwise, false.}
   * @param isStale
   */
  public void setStale(boolean isStale) {
    this.isStale = isStale;
  }

  /**
   * Sets the total results for this query
   * @param totalResults
   */
  public void setTotalResults(int totalResults) {
    this.totalResults = totalResults;
  }
}
