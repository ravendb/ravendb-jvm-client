package net.ravendb.client.documents.session;

/**
 * A query against a Raven index
 */
public interface IDocumentQuery<T> extends IDocumentQueryBase<T, IDocumentQuery<T>>, IDocumentQueryBaseSingle<T>, IEnumerableQuery<T> { //TODO: extends iterator

    /* TODO
  string IndexName { get; }

        /// <summary>
        ///     Whether we should apply distinct operation to the query on the server side
        /// </summary>
        bool IsDistinct { get; }

        /// <summary>
        ///     Returns the query result. Accessing this property for the first time will execute the query.
        /// </summary>
        QueryResult GetQueryResult();

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

        /// <summary>
        ///     Ability to use one factory to determine spatial shape that will be used in query.
        /// </summary>
        /// <param name="path">Spatial field name.</param>
        /// <param name="clause">function with spatial criteria factory</param>
        IDocumentQuery<T> Spatial(Expression<Func<T, object>> path, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

        /// <summary>
        ///     Ability to use one factory to determine spatial shape that will be used in query.
        /// </summary>
        /// <param name="fieldName">Spatial field name.</param>
        /// <param name="clause">function with spatial criteria factory</param>
        IDocumentQuery<T> Spatial(string fieldName, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

        IDocumentQuery<T> Spatial(SpatialDynamicField field, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

        IDocumentQuery<T> Spatial(Func<SpatialDynamicFieldFactory<T>, SpatialDynamicField> field, Func<SpatialCriteriaFactory, SpatialCriteria> clause);

        /// <summary>
        /// Get the facets as per the specified facet document with the given start and pageSize
        /// </summary>
        FacetedQueryResult GetFacets(string facetSetupDoc, int start, int? pageSize);

        /// <summary>
        /// Get the facet results as per the specified facets with the given start and pageSize
        /// </summary>
        FacetedQueryResult GetFacets(List<Facet> facets, int start, int? pageSize);

        /// <summary>
        ///     Get the facets lazily as per the specified doc with the given start and pageSize
        /// </summary>
        Lazy<FacetedQueryResult> GetFacetsLazy(string facetSetupDoc, int facetStart, int? facetPageSize);

        /// <summary>
        ///     Get the facets lazily as per the specified doc with the given start and pageSize
        /// </summary>
        Lazy<FacetedQueryResult> GetFacetsLazy(List<Facet> facets, int facetStart, int? facetPageSize);



        /// <summary>
        /// Changes the return type of the query
        /// </summary>
        IDocumentQuery<TResult> OfType<TResult>();

        IGroupByDocumentQuery<T> GroupBy(string fieldName, params string[] fieldNames);
     */

}
