package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.facets.FacetResult;

import java.util.Map;

public interface IRawDocumentQuery<T> extends IQueryBase<T, IRawDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /**
     * Add a named parameter to the query
     * @return Raw Document Query
     */
    IRawDocumentQuery<T> addParameter(String name, Object value);

    /**
     * Execute raw query aggregated by facet
     * @return Facet as map
     */
    Map<String, FacetResult> executeAggregation();
}
