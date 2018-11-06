package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.explanation.ExplanationOptions;
import net.ravendb.client.documents.queries.explanation.Explanations;
import net.ravendb.client.documents.queries.highlighting.HighlightingOptions;
import net.ravendb.client.documents.queries.highlighting.Highlightings;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.session.loaders.IQueryIncludeBuilder;
import net.ravendb.client.primitives.Reference;

import java.util.function.Consumer;

/**
 *  A query against a Raven index
 */
public interface IDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> extends IQueryBase<T, TSelf>, IFilterDocumentQueryBase<T, TSelf> {

    /**
     * Adds an ordering for a specific field to the query
     * @param fieldName Field name
     * @param descending use descending order
     * @return Query instance
     */
    TSelf addOrder(String fieldName, boolean descending);

    /**
     * Adds an ordering for a specific field to the query
     * @param fieldName Field name
     * @param descending use descending order
     * @param ordering ordering type
     * @return Query instance
     */
    TSelf addOrder(String fieldName, boolean descending, OrderingType ordering);

    //TBD expr TSelf AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending = false, OrderingType ordering = OrderingType.String);

    /**
     * Specifies a boost weight to the last where clause.
     * The higher the boost factor, the more relevant the term will be.
     *
     * boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
     * @param boost Boost value
     * @return Query instance
     */
    TSelf boost(double boost);

    /**
     * Apply distinct operation to this query
     * @return Query instance
     */
    TSelf distinct();

    /**
     * Adds explanations of scores calculated for queried documents to the query result
     * @param explanations Output parameter
     * @return Query instance
     */
    TSelf includeExplanations(Reference<Explanations> explanations);

    /**
     * Adds explanations of scores calculated for queried documents to the query result
     * @param options Options
     * @param explanations Output parameter
     * @return Query instance
     */
    TSelf includeExplanations(ExplanationOptions options, Reference<Explanations> explanations);

    /**
     * Specifies a fuzziness factor to the single word term in the last where clause
     * 0.0 to 1.0 where 1.0 means closer match
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
     * @param fuzzy Fuzzy value
     * @return Query instance
     */
    TSelf fuzzy(double fuzzy);

    TSelf highlight(String fieldName, int fragmentLength, int fragmentCount, Reference<Highlightings> highlightings);
    TSelf highlight(String fieldName, int fragmentLength, int fragmentCount, HighlightingOptions options, Reference<Highlightings> highlightings);
    //TBD expr TSelf Highlight(Expression<Func<T, object>> path, int fragmentLength, int fragmentCount, out Highlightings highlightings);
    //TBD expr TSelf Highlight(Expression<Func<T, object>> path, int fragmentLength, int fragmentCount, HighlightingOptions options, out Highlightings highlightings);

    /**
     * Includes the specified path in the query, loading the document specified in that path
     * @param path Path to include
     * @return Query instance
     */
    TSelf include(String path);

    TSelf include(Consumer<IQueryIncludeBuilder> includes);

    //TBD expr TSelf Include(Expression<Func<T, object>> path);

    /**
     * Partition the query so we can intersect different parts of the query
     *  across different index entries.
     *  @return Query instance
     */
    TSelf intersect();

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     * @param field Field to use in order by
     * @return Query instance
     */
    TSelf orderBy(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     * @param field Field to use in order by
     * @param ordering Ordering type
     * @return Query instance
     */
    TSelf orderBy(String field, OrderingType ordering);

    //TBD expr TSelf OrderBy<TValue>(params Expression<Func<T, TValue>>[] propertySelectors);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     * @param field Field to use in order by
     * @return Query instance
     */
    TSelf orderByDescending(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     * @param field Field to use in order by
     * @param ordering Ordering type
     * @return Query instance
     */
    TSelf orderByDescending(String field, OrderingType ordering);

    //TBD expr TSelf OrderByDescending<TValue>(params Expression<Func<T, TValue>>[] propertySelectors);

    /**
     * Adds an ordering by score for a specific field to the query
     * @return Query instance
     */
    TSelf orderByScore();

    /**
     * Adds an ordering by score for a specific field to the query
     * @return Query instance
     */
    TSelf orderByScoreDescending();

    /**
     * Specifies a proximity distance for the phrase in the last search clause
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
     * @param proximity Proximity value
     * @return Query instance
     */
    TSelf proximity(int proximity);

    /**
     * Order the search results randomly
     * @return Query instance
     */
    TSelf randomOrdering();

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     * @param seed Seed to use
     * @return Query instance
     */
    TSelf randomOrdering(String seed);

    //TBD 4.1 TSelf customSortUsing(String typeName, boolean descending);

    /**
     * Sorts the query results by distance.
     * @param field Field to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Query instance
     */
    TSelf orderByDistance(DynamicSpatialField field, double latitude, double longitude);

    //TBD expr TSelf OrderByDistance(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, double latitude, double longitude);

    TSelf orderByDistance(DynamicSpatialField field, String shapeWkt);

    //TBD expr TSelf OrderByDistance(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, string shapeWkt);

    //TBD expr  TSelf OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude);

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Query instance
     */
    TSelf orderByDistance(String fieldName, double latitude, double longitude);

    //TBD expr TSelf OrderByDistance<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt);

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use in order by
     * @param shapeWkt WKT shape to use
     * @return Query instance
     */
    TSelf orderByDistance(String fieldName, String shapeWkt);

    /**
     * Sorts the query results by distance.
     * @param field Field to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Query instance
     */
    TSelf orderByDistanceDescending(DynamicSpatialField field, double latitude, double longitude);

    //TBD expr TSelf OrderByDistanceDescending(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, double latitude, double longitude);

    TSelf orderByDistanceDescending(DynamicSpatialField field, String shapeWkt);

    //TBD expr TSelf OrderByDistanceDescending(Func<DynamicSpatialFieldFactory<T>, DynamicSpatialField> field, string shapeWkt);

    //TBD expr TSelf OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, double latitude, double longitude);

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Query instance
     */
    TSelf orderByDistanceDescending(String fieldName, double latitude, double longitude);

    //TBD expr TSelf OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt);

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use
     * @param shapeWkt WKT shape to use
     * @return Query instance
     */
    TSelf orderByDistanceDescending(String fieldName, String shapeWkt);
}
