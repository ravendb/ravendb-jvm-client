package net.ravendb.client.documents.queries;

import java.util.Date;

public abstract class QueryResultBase<TResult, TInclude> {

    private TResult results;

    private TInclude includes;

    private String[] includedPaths;

    private boolean isStale;

    private Date indexTimestamp;

    private String indexName;

    private Long resultEtag;

    private Date lastQueryTime;

    /**
     * Gets the document resulting from this query.
     */
    public TResult getResults() {
        return results;
    }

    /**
     * Sets the document resulting from this query.
     */
    public void setResults(TResult results) {
        this.results = results;
    }

    /**
     * Gets the document included in the result.
     */
    public TInclude getIncludes() {
        return includes;
    }

    /**
     * Sets the document included in the result.
     */
    public void setIncludes(TInclude includes) {
        this.includes = includes;
    }

    /**
     * The paths that the server included in the results
     */
    public String[] getIncludedPaths() {
        return includedPaths;
    }

    /**
     * The paths that the server included in the results
     */
    public void setIncludedPaths(String[] includedPaths) {
        this.includedPaths = includedPaths;
    }

    /**
     * Gets a value indicating whether the index is stale.
     */
    public boolean isStale() {
        return isStale;
    }

    /**
     * Sets a value indicating whether the index is stale.
     */
    public void setStale(boolean stale) {
        isStale = stale;
    }

    /**
     * The last time the index was updated.
     * This can be used to determine the freshness of the data.
     */
    public Date getIndexTimestamp() {
        return indexTimestamp;
    }

    /**
     * The last time the index was updated.
     * This can be used to determine the freshness of the data.
     */
    public void setIndexTimestamp(Date indexTimestamp) {
        this.indexTimestamp = indexTimestamp;
    }

    /**
     * The index used to answer this query
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The index used to answer this query
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * The ETag value for this index current state, which include what docs were indexed,
     * what document were deleted, etc.
     */
    public Long getResultEtag() {
        return resultEtag;
    }

    /**
     * The ETag value for this index current state, which include what docs were indexed,
     * what document were deleted, etc.
     */
    public void setResultEtag(Long resultEtag) {
        this.resultEtag = resultEtag;
    }

    /**
     * The timestamp of the last time the index was queried
     */
    public Date getLastQueryTime() {
        return lastQueryTime;
    }

    /**
     * The timestamp of the last time the index was queried
     */
    public void setLastQueryTime(Date lastQueryTime) {
        this.lastQueryTime = lastQueryTime;
    }
}
