package net.ravendb.client.documents.queries;

import net.ravendb.client.Parameters;

import java.io.IOException;
import java.util.Optional;

/**
 * All the information required to query an index
 */
public class IndexQuery extends IndexQueryWithParameters<Parameters> {

    public IndexQuery() {
    }

    public IndexQuery(String query) {
        this.setQuery(query);
    }

    private boolean disableCaching;

    /**
     * Indicates if query results should be read from cache (if cached previously) or added to cache (if there were no cached items prior)
     * @return true if caching was disabled
     */
    public boolean isDisableCaching() {
        return disableCaching;
    }

    /**
     * Indicates if query results should be read from cache (if cached previously) or added to cache (if there were no cached items prior)
     * @param disableCaching sets the value
     */
    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    public String getQueryHash() {
        HashCalculator hasher = new HashCalculator();
        try {
            hasher.write(getQuery());
            hasher.write(isWaitForNonStaleResults());
            hasher.write(isSkipDuplicateChecking());
            hasher.write(Optional.ofNullable(getWaitForNonStaleResultsTimeout()).map(x -> x.toMillis()).orElse(0L));
            hasher.write(getStart());
            hasher.write(getPageSize());
            hasher.write(getQueryParameters());
            return hasher.getHash();
        } catch (IOException e) {
            throw new RuntimeException("Unable to calculate hash", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IndexQuery that = (IndexQuery) o;

        return disableCaching == that.disableCaching;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (disableCaching ? 1 : 0);
        return result;
    }
}
