package net.ravendb.client.documents.queries.facets;

public interface IFacetOperations<T> extends IFacetOperationsBase<T, IFacetOperations<T>> {
    IFacetOperations<T> withOptions(FacetOptions options);
}
