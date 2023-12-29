package net.ravendb.client.documents.queries.facets;

public interface IFacetOperationsBase<T, TSelf> {
    TSelf withDisplayName(String displayName);
    TSelf sumOn(String path);
    TSelf sumOn(String path, String displayName);
    TSelf minOn(String path);
    TSelf minOn(String path, String displayName);
    TSelf maxOn(String path);
    TSelf maxOn(String path, String displayName);
    TSelf averageOn(String path);
    TSelf averageOn(String path, String displayName);
}
