package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;

/**
 *  A query against a Raven index
 */
public interface IDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> extends IQueryBase<T, TSelf>, IFilterDocumentQueryBase<T, TSelf> {

    /**
     * Adds an ordering for a specific field to the query
     */
    TSelf addOrder(String fieldName, boolean descending);

    /**
     * Adds an ordering for a specific field to the query
     */
    TSelf addOrder(String fieldName, boolean descending, OrderingType ordering);

    //TBD TSelf AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending = false, OrderingType ordering = OrderingType.String);

    /**
     * Specifies a boost weight to the last where clause.
     * The higher the boost factor, the more relevant the term will be.
     *
     * boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
     */
    TSelf boost(double boost);

    /**
     * Apply distinct operation to this query
     */
    TSelf distinct();

    /**
     * Adds explanations of scores calculated for queried documents to the query result
     */
    TSelf explainScores();

    /**
     * Specifies a fuzziness factor to the single word term in the last where clause
     * 0.0 to 1.0 where 1.0 means closer match
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
     */
    TSelf fuzzy(double fuzzy);

    //TBD TSelf Highlight(string fieldName, int fragmentLength, int fragmentCount, string fragmentsField);

    //TBD TSelf Highlight(string fieldName, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);

    //TBD TSelf Highlight(string fieldName, string fieldKeyName, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);

    //TBD TSelf Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, int fragmentLength, int fragmentCount, Expression<Func<T, IEnumerable>> fragmentsPropertySelector);

    //TBD TSelf Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);

    //TBD TSelf Highlight<TValue>(Expression<Func<T, TValue>> propertySelector, Expression<Func<T, TValue>> keyPropertySelector, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);

    /**
     * Includes the specified path in the query, loading the document specified in that path
     */
    TSelf include(String path);

    //TBD TSelf Include(Expression<Func<T, object>> path);

    /**
     * Partition the query so we can intersect different parts of the query
     *  across different index entries.
     */
    TSelf intersect();

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     */
    TSelf orderBy(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     */
    TSelf orderBy(String field, OrderingType ordering);

    //TBD TSelf OrderBy<TValue>(params Expression<Func<T, TValue>>[] propertySelectors);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     */
    TSelf orderByDescending(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     */
    TSelf orderByDescending(String field, OrderingType ordering);

    //TBD TSelf OrderByDescending<TValue>(params Expression<Func<T, TValue>>[] propertySelectors);

    /**
     * Adds an ordering by score for a specific field to the query
     */
    TSelf orderByScore();

    /**
     * Adds an ordering by score for a specific field to the query
     */
    TSelf orderByScoreDescending();

    /**
     * Specifies a proximity distance for the phrase in the last where clause
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
     */
    TSelf proximity(int proximity);

    /**
     * Order the search results randomly
     */
    TSelf randomOrdering();

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     */
    TSelf randomOrdering(String seed);

    //TBD TSelf customSortUsing(String typeName, boolean descending);

    //TBD TSelf SetHighlighterTags(string preTag, string postTag);
    //TBD TSelf SetHighlighterTags(string[] preTags, string[] postTags);


    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistance(DynamicSpatialField field, double latitude, double longitude);

    //TBD TSelf OrderByDistance(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, double latitude, double longitude);

    TSelf orderByDistance(DynamicSpatialField field, String shapeWkt);

    //TBD TSelf OrderByDistance(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, string shapeWkt);

    //TBD TSelf OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude);

    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistance(String fieldName, double latitude, double longitude);

    //TBD TSelf OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt);

    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistance(String fieldName, String shapeWkt);

    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistanceDescending(DynamicSpatialField field, double latitude, double longitude);

    //TBD TSelf OrderByDistanceDescending(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, double latitude, double longitude);

    TSelf orderByDistanceDescending(DynamicSpatialField field, String shapeWkt);

    //TBD TSelf OrderByDistanceDescending(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, string shapeWkt);

    //TBD TSelf OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude);

    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistanceDescending(String fieldName, double latitude, double longitude);

    //TBD TSelf OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt);

    /**
     * Sorts the query results by distance.
     */
    TSelf orderByDistanceDescending(String fieldName, String shapeWkt);
}
