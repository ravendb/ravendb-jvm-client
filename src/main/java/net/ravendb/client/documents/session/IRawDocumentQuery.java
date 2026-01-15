package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.ProjectionBehavior;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.DocumentationUrls;
import java.util.Map;

/**
 * Allows to express a query directly in RQL using string containing query syntax.
 * @param <T> Query result type
 * {@inheritDoc}
 * @see DocumentationUrls.Session.Querying#RawDocumentQuery
 */
public interface IRawDocumentQuery<T>
        extends IQueryBase<T, IRawDocumentQuery<T>>, IPagingDocumentQueryBase<T, IRawDocumentQuery<T>>,
        IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    /**
     * Add a named parameter to the query
     * @return Raw Document Query
     */
    IRawDocumentQuery<T> addParameter(String name, Object value);

    /**
     * Allows to change the projection behavior of a query. The projection behavior allows to control where RavenDB will try to retrieve the fields values from.
     * @param projectionBehavior Desired projection behavior type
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#ProjectionBehavior
     */
    IRawDocumentQuery<T> projection(ProjectionBehavior projectionBehavior);

    /**
     * Execute raw query aggregated by facet
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#FacetedSearch
     * @return Dictionary with declared facet names keys and corresponding facet aggregation values
     */
    Map<String, FacetResult> executeAggregation();
}
