package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.ProjectionBehavior;
import net.ravendb.client.documents.queries.facets.FacetResult;

import java.util.Map;

public interface IRawDocumentQuery<T> extends IQueryBase<T, IRawDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /**
     * Add a named parameter to the query
     */
    IRawDocumentQuery<T> addParameter(String name, Object value);

    IRawDocumentQuery<T> projection(ProjectionBehavior projectionBehavior);

    /**
     * Execute raw query aggregated by facet
     */
    Map<String, FacetResult> executeAggregation();
}
