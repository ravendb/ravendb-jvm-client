package net.ravendb.client.documents.queries;

import net.ravendb.client.Parameters;

/**
 * All the information required to query an index
 */
public class IndexQuery extends IndexQueryWithParameters<Parameters> {

    private boolean disableCaching;

    /**
     * Indicates if query results should be read from cache (if cached previously) or added to cache (if there were no cached items prior)
     */
    public boolean isDisableCaching() {
        return disableCaching;
    }

    /**
     * Indicates if query results should be read from cache (if cached previously) or added to cache (if there were no cached items prior)
     */
    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    /* TODO

        public ulong GetQueryHash(JsonOperationContext ctx)
        {
            using (var hasher = new QueryHashCalculator(ctx))
            {
                hasher.Write(Query);
                hasher.Write(WaitForNonStaleResults);
                hasher.Write(SkipDuplicateChecking);
                hasher.Write(ShowTimings);
                hasher.Write(ExplainScores);
                hasher.Write(WaitForNonStaleResultsTimeout?.Ticks);
                hasher.Write(CutoffEtag);
                hasher.Write(Start);
                hasher.Write(PageSize);
                hasher.Write(QueryParameters);

                return hasher.GetHash();
            }
        }*/

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
