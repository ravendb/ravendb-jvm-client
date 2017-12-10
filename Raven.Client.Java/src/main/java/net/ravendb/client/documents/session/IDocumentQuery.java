package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.GroupByMethod;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialCriteriaFactory;
import net.ravendb.client.documents.queries.spatial.SpatialDynamicField;
import net.ravendb.client.primitives.Tuple;

import java.util.function.Function;

/**
 * A query against a Raven index
 */
public interface IDocumentQuery<T> extends IDocumentQueryBase<T, IDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> { //TODO: extends iterator

    String getIndexName();

    /**
     * Whether we should apply distinct operation to the query on the server side
     */
    boolean isDistinct();

    /**
     * Returns the query result. Accessing this property for the first time will execute the query.
     */
    QueryResult getQueryResult();
    /* TODO

        /// <summary>
        ///     Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
        ///     will come from document directly.
        /// </summary>
        /// <typeparam name="TProjection">Type of the projection.</typeparam>
        /// <param name="fields">Array of fields to load.</param>
        IDocumentQuery<TProjection> SelectFields<TProjection>(params string[] fields);

        /// <summary>
        ///     Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
        ///     will come from document directly.
        /// </summary>
        /// <typeparam name="TProjection">Type of the projection.</typeparam>
        /// <param name="queryData">An object containing the fields to load, field projections and a From-Token alias name</param>
        IDocumentQuery<TProjection> SelectFields<TProjection>(QueryData queryData);

        /// <summary>
        ///     Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value
        ///     will come from document directly.
        ///     <para>Array of fields will be taken from TProjection</para>
        /// </summary>
        /// <typeparam name="TProjection">Type of the projection from which fields will be taken.</typeparam>
        IDocumentQuery<TProjection> SelectFields<TProjection>();
*/
    //TBD IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

    /**
     * Ability to use one factory to determine spatial shape that will be used in query.
     */
    IDocumentQuery<T> spatial(String fieldName, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    IDocumentQuery<T> spatial(SpatialDynamicField field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    //TBD IDocumentQuery<T> spatial(Function<SpatialDynamicFieldFactory<T>, SpatialDynamicField> field, Function<SpatialCriteriaFactory, SpatialCriteria> clause);

    /**
     * Changes the return type of the query
     */
    <TResult> IDocumentQuery<TResult> ofType(Class<TResult> resultClass);

    IGroupByDocumentQuery<T> groupBy(String fieldName, String... fieldNames);

    IGroupByDocumentQuery<T> groupBy(Tuple<String, GroupByMethod> field, Tuple<String, GroupByMethod>... fields);

    //TBD MoreLikeThis
    //TBD AggregateBy
    //TBD SuggestUsing

}
