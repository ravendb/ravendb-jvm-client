package net.ravendb.client.documents.queries.facets;

public interface IFacetOperations<T> extends IFacetOperationsBase<T, IFacetOperations<T>> {
    /**
     * Optional configuration for the facet query.
     *
     * @param options Configuration object. See more at {@link FacetOptions}.
     * @return the result of the facet query (if applicable)
     */
    IFacetOperations<T> withOptions(FacetOptions options);
}
