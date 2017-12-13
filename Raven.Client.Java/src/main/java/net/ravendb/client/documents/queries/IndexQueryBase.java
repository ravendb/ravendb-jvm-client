package net.ravendb.client.documents.queries;

import java.time.Duration;

public class IndexQueryBase<T> implements IIndexQuery {

    private int _pageSize = Integer.MAX_VALUE;
    private boolean pageSizeSet;
    private String query;
    private T queryParameters;
    private int start;
    private boolean waitForNonStaleResults;
    private Duration waitForNonStaleResultsTimeout;
    private Long cutoffEtag;

    /**
     * Whether the page size was explicitly set or still at its default value
     */
    public boolean isPageSizeSet() {
        return pageSizeSet;
    }

    /**
     * Actual query that will be performed (Lucene syntax).
     */
    public String getQuery() {
        return query;
    }

    /**
     * Actual query that will be performed (Lucene syntax).
     */
    public void setQuery(String query) {
        this.query = query;
    }

    public T getQueryParameters() {
        return queryParameters;
    }

    public void setQueryParameters(T queryParameters) {
        this.queryParameters = queryParameters;
    }

    /**
     * Number of records that should be skipped.
     */
    public int getStart() {
        return start;
    }

    /**
     * Number of records that should be skipped.
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * Maximum number of records that will be retrieved.
     */
    public int getPageSize() {
        return _pageSize;
    }

    /**
     * Maximum number of records that will be retrieved.
     */
    public void setPageSize(int pageSize) {
        _pageSize = pageSize;
        pageSizeSet = true;
    }

    /**
     * When set to true server side will wait until result are non stale or until timeout
     */
    public boolean isWaitForNonStaleResults() {
        return waitForNonStaleResults;
    }

    /**
     * When set to true server side will wait until result are non stale or until timeout
     */
    public void setWaitForNonStaleResults(boolean waitForNonStaleResults) {
        this.waitForNonStaleResults = waitForNonStaleResults;
    }

    @Override
    public Duration getWaitForNonStaleResultsTimeout() {
        return waitForNonStaleResultsTimeout;
    }

    public void setWaitForNonStaleResultsTimeout(Duration waitForNonStaleResultsTimeout) {
        this.waitForNonStaleResultsTimeout = waitForNonStaleResultsTimeout;
    }

    @Override
    public String toString() {
        return query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexQueryBase<?> that = (IndexQueryBase<?>) o;

        if (_pageSize != that._pageSize) return false;
        if (pageSizeSet != that.pageSizeSet) return false;
        if (start != that.start) return false;
        if (waitForNonStaleResults != that.waitForNonStaleResults) return false;
        if (query != null ? !query.equals(that.query) : that.query != null) return false;
        if (waitForNonStaleResultsTimeout != null ? !waitForNonStaleResultsTimeout.equals(that.waitForNonStaleResultsTimeout) : that.waitForNonStaleResultsTimeout != null)
            return false;
        return cutoffEtag != null ? cutoffEtag.equals(that.cutoffEtag) : that.cutoffEtag == null;
    }

    @Override
    public int hashCode() {
        int result = _pageSize;
        result = 31 * result + (pageSizeSet ? 1 : 0);
        result = 31 * result + (query != null ? query.hashCode() : 0);
        result = 31 * result + start;
        result = 31 * result + (waitForNonStaleResults ? 1 : 0);
        result = 31 * result + (waitForNonStaleResultsTimeout != null ? waitForNonStaleResultsTimeout.hashCode() : 0);
        result = 31 * result + (cutoffEtag != null ? cutoffEtag.hashCode() : 0);
        return result;
    }


}
