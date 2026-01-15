package net.ravendb.client.documents.queries.facets;

public interface IFacetBuilder<T> {

    IFacetOperations<T> byRanges(RangeBuilder range, RangeBuilder... ranges);
    /**
     * Returns a count for each unique term found in the specified index field.
     *
     * @param fieldName Name of the field from the index.
     */
    IFacetOperations<T> byField(String fieldName);
    /**
     * Scopes all index results. Useful to gather index-wide statistics data.
     *
     * @return the scoped index results
     */
    IFacetOperations<T> allResults();

    //TBD expr IFacetOperations<T> ByField(Expression<Func<T, object>> path);
    //TBD expr IFacetOperations<T> ByRanges(Expression<Func<T, bool>> path, params Expression<Func<T, bool>>[] paths);
}
