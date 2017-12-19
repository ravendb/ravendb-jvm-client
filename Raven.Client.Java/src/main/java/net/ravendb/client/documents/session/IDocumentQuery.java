package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.GroupBy;
import net.ravendb.client.documents.queries.QueryData;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialCriteriaFactory;

import java.util.function.Function;

/**
 * A query against a Raven index
 */
public interface IDocumentQuery<T> extends IDocumentQueryBase<T, IDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> {

    String getIndexName();

    /**
     * Whether we should apply distinct operation to the query on the server side
     */
    boolean isDistinct();

    /**
     * Returns the query result. Accessing this property for the first time will execute the query.
     */
    QueryResult getQueryResult();

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass);

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String... fields);

    /**
     * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
     * will come from document directly.
     */
    <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, QueryData queryData);

    /**
     * Changes the return type of the query
     */
    <TResult> IDocumentQuery<TResult> ofType(Class<TResult> resultClass);

    IGroupByDocumentQuery<T> groupBy(String fieldName, String... fieldNames);

    IGroupByDocumentQuery<T> groupBy(GroupBy field, GroupBy... fields);

    //TBD MoreLikeThis
    //TBD AggregateBy
    //TBD SuggestUsing

}
