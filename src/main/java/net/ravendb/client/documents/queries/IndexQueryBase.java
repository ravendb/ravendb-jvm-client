package net.ravendb.client.documents.queries;

import java.time.Duration;

public class IndexQueryBase<T> implements IIndexQuery {

    private String query;
    private T queryParameters;
    private ProjectionBehavior projectionBehavior;
    private boolean waitForNonStaleResults;
    private Duration waitForNonStaleResultsTimeout;

    /**
     * Actual query that will be performed (RQL syntax)
     * @return Index query
     */
    public String getQuery() {
        return query;
    }

    /**
     * Actual query that will be performed (RQL syntax)
     * @param query Sets the value
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

    public ProjectionBehavior getProjectionBehavior() {
        return projectionBehavior;
    }

    public void setProjectionBehavior(ProjectionBehavior projectionBehavior) {
        this.projectionBehavior = projectionBehavior;
    }

    /**
     * When set to true server side will wait until result are non stale or until timeout
     * @return true if server should wait for non stale results
     */
    public boolean isWaitForNonStaleResults() {
        return waitForNonStaleResults;
    }

    /**
     * When set to true server side will wait until result are non stale or until timeout
     * @param waitForNonStaleResults Sets the valueQer
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

        if (waitForNonStaleResults != that.waitForNonStaleResults) return false;
        if (query != null ? !query.equals(that.query) : that.query != null) return false;
        return waitForNonStaleResultsTimeout != null ? waitForNonStaleResultsTimeout.equals(that.waitForNonStaleResultsTimeout) : that.waitForNonStaleResultsTimeout == null;
    }

    @Override
    public int hashCode() {
        int result = (query != null ? query.hashCode() : 0);
        result = 31 * result + (waitForNonStaleResults ? 1 : 0);
        result = 31 * result + (waitForNonStaleResultsTimeout != null ? waitForNonStaleResultsTimeout.hashCode() : 0);
        return result;
    }


}
