package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.GroupBy;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.facets.Facet;
import net.ravendb.client.documents.queries.facets.FacetBase;
import net.ravendb.client.documents.queries.facets.IAggregationDocumentQuery;
import net.ravendb.client.documents.queries.facets.IFacetBuilder;
import net.ravendb.client.documents.queries.moreLikeThis.IMoreLikeThisBuilderForDocumentQuery;
import net.ravendb.client.documents.queries.suggestions.ISuggestionBuilder;
import net.ravendb.client.documents.queries.suggestions.ISuggestionDocumentQuery;
import net.ravendb.client.documents.queries.suggestions.SuggestionBase;

import java.util.function.Consumer;

/**
 * A query against a Raven index
 */
public interface IDocumentQuery<T> extends IDocumentQueryBase<T, IDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    String getIndexName();

    Class<T> getQueryClass();

    /**
     * Whether we should apply distinct operation to the query on the server side
     * @return true if server should return distinct results
     */
    boolean isDistinct();

    /**
     * Returns the query result. Accessing this property for the first time will execute the query.
     * @return query result
     */
    QueryResult getQueryResult();

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     * @param <TProjection> projection class
     * @param projectionClass projection class
     * @return Document query
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass);

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     * @param <TProjection> projection class
     * @param projectionClass projection class
     * @param fields Fields to fetch
     * @return Document query
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String... fields);

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     * @param <TProjection> projection class
     * @param projectionClass projection class
     * @param queryData Query data to use
     * @return Document query
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, QueryData queryData);

    /**
     * Changes the return type of the query
     * @param <TResult> class of result
     * @param resultClass class of result
     * @return Document query
     */
    <TResult> IDocumentQuery<TResult> ofType(Class<TResult> resultClass);

    IGroupByDocumentQuery<T> groupBy(String fieldName, String... fieldNames);

    IGroupByDocumentQuery<T> groupBy(GroupBy field, GroupBy... fields);

    IDocumentQuery<T> moreLikeThis(Consumer<IMoreLikeThisBuilderForDocumentQuery<T>> builder);

    IAggregationDocumentQuery<T> aggregateBy(Consumer<IFacetBuilder<T>> builder);

    IAggregationDocumentQuery<T> aggregateBy(FacetBase facet);

    IAggregationDocumentQuery<T> aggregateBy(Facet... facet);

    IAggregationDocumentQuery<T> aggregateUsing(String facetSetupDocumentId);

    ISuggestionDocumentQuery<T> suggestUsing(SuggestionBase suggestion);

    ISuggestionDocumentQuery<T> suggestUsing(Consumer<ISuggestionBuilder<T>> builder);
}
