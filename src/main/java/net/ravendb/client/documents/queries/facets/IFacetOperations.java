package net.ravendb.client.documents.queries.facets;

public interface IFacetOperations<T> {

    IFacetOperations<T> withDisplayName(String displayName);

    IFacetOperations<T> withOptions(FacetOptions options);

    IFacetOperations<T> sumOn(String path);
    IFacetOperations<T> minOn(String path);
    IFacetOperations<T> maxOn(String path);
    IFacetOperations<T> averageOn(String path);

    //TBD expr overloads with expression
}
