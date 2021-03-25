package net.ravendb.client.documents.queries;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;
import java.util.Map;

public abstract class QueryResultBase<TResult, TInclude> {

    private TResult results;

    private TInclude includes;

    private String[] includedPaths;

    private boolean isStale;

    private Date indexTimestamp;

    private String indexName;

    private Long resultEtag;

    private Date lastQueryTime;

    private String nodeTag;

    /**
     * Gets the document resulting from this query.
     * @return Query results
     */
    public TResult getResults() {
        return results;
    }

    /**
     * Sets the document resulting from this query.
     * @param results Sets the query results
     */
    public void setResults(TResult results) {
        this.results = results;
    }

    /**
     * Gets the document included in the result.
     * @return Query includes
     */
    public TInclude getIncludes() {
        return includes;
    }

    /**
     * Sets the document included in the result.
     * @param includes Sets the value
     */
    public void setIncludes(TInclude includes) {
        this.includes = includes;
    }

    /**
     * The paths that the server included in the results
     * @return Included paths
     */
    public String[] getIncludedPaths() {
        return includedPaths;
    }

    /**
     * The paths that the server included in the results
     * @param includedPaths Sets the value
     */
    public void setIncludedPaths(String[] includedPaths) {
        this.includedPaths = includedPaths;
    }

    /**
     * Gets a value indicating whether the index is stale.
     * @return true if index results are stale
     */
    public boolean isStale() {
        return isStale;
    }

    /**
     * Sets a value indicating whether the index is stale.
     * @param stale Sets the value
     */
    public void setStale(boolean stale) {
        isStale = stale;
    }

    /**
     * The last time the index was updated.
     * This can be used to determine the freshness of the data.
     * @return index timestamp
     */
    public Date getIndexTimestamp() {
        return indexTimestamp;
    }

    /**
     * The last time the index was updated.
     * This can be used to determine the freshness of the data.
     * @param indexTimestamp Sets the value
     */
    public void setIndexTimestamp(Date indexTimestamp) {
        this.indexTimestamp = indexTimestamp;
    }

    /**
     * The index used to answer this query
     * @return Used index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * The index used to answer this query
     * @param indexName Sets the value
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * The ETag value for this index current state, which include what docs were indexed,
     * what document were deleted, etc.
     * @return result etag
     */
    public Long getResultEtag() {
        return resultEtag;
    }

    /**
     * The ETag value for this index current state, which include what docs were indexed,
     * what document were deleted, etc.
     * @param resultEtag Sets the value
     */
    public void setResultEtag(Long resultEtag) {
        this.resultEtag = resultEtag;
    }

    /**
     * The timestamp of the last time the index was queried
     * @return Last query time
     */
    public Date getLastQueryTime() {
        return lastQueryTime;
    }

    /**
     * The timestamp of the last time the index was queried
     * @param lastQueryTime Sets the value
     */
    public void setLastQueryTime(Date lastQueryTime) {
        this.lastQueryTime = lastQueryTime;
    }

    /**
     * @return Tag of a cluster node which responded to the query
     */
    public String getNodeTag() {
        return nodeTag;
    }

    /**
     * @param nodeTag Tag of a cluster node which responded to the query
     */
    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }


}
