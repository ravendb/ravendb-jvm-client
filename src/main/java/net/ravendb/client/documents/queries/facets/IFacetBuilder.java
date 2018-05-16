package net.ravendb.client.documents.queries.facets;

public interface IFacetBuilder<T> {

    IFacetOperations<T> byRanges(RangeBuilder range, RangeBuilder... ranges);

    IFacetOperations<T> byField(String fieldName);

    IFacetOperations<T> allResults();

    //TBD expr IFacetOperations<T> ByField(Expression<Func<T, object>> path);
    //TBD expr IFacetOperations<T> ByRanges(Expression<Func<T, bool>> path, params Expression<Func<T, bool>>[] paths);
}
