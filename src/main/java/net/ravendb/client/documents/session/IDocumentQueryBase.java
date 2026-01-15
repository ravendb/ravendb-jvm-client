package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.explanation.ExplanationOptions;
import net.ravendb.client.documents.queries.explanation.Explanations;
import net.ravendb.client.documents.queries.highlighting.HighlightingOptions;
import net.ravendb.client.documents.queries.highlighting.Highlightings;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.session.loaders.IQueryIncludeBuilder;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.DocumentationUrls;
import java.util.function.Consumer;

/**
 *  Interface providing low-level querying capabilities.
 * {@inheritDoc}
 * @see DocumentationUrls.Session.Querying#QueryVsDocumentQuery
 */
public interface IDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>>
        extends IQueryBase<T, TSelf>, IFilterDocumentQueryBase<T, TSelf>, IPagingDocumentQueryBase<T, TSelf> {

    /**
     * Orders query results by specified field.
     * @param fieldName Name of the field to order the query results by.
     * @param descending Specifies if order is descending. Default: false.
     */
    TSelf addOrder(String fieldName, boolean descending);

    /**
     * {@inheritDoc}
     * @see #addOrder(String, boolean)
     * @param ordering Ordering type. Default: OrderingType.String.
     */
    TSelf addOrder(String fieldName, boolean descending, OrderingType ordering);

    //TBD expr TSelf AddOrder<TValue>(Expression<Func<T, TValue>> propertySelector, bool descending = false, OrderingType ordering = OrderingType.String);

    /**
     * Specifies boost weight for the preceding Where clause.
     * The higher the boost weight, the more relevant the term will be.
     * By default all terms have weight of 1.0.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#BoostSearchResults
     * @param boost Boost weight.
     */
    TSelf boost(double boost);

    /**
     * Removes duplicates from query results.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#Distinct
     */
    TSelf distinct();

    /**
     * Adds explanations of scores calculated for queried documents to the query result
     * @param explanations Output parameter
     * @return Query instance
     */
    TSelf includeExplanations(Reference<Explanations> explanations);

    /**
     * Explanations gives context how document was matched by query and provide information about how the score was calculated.
     * @param options Options Additional explanation configuration
     * @param explanations Out parameter where explanations will be returned
     * @return Query instance
     */
    TSelf includeExplanations(ExplanationOptions options, Reference<Explanations> explanations);

    /**
     * Specifies a fuzziness factor for the preceding WhereEquals clause,
     * making it match documents containing terms similar to searched one.
     * The higher the factor, the more similar terms will be matched.
     * {@inheritDoc}
     * @see DocumentationUrls.Session.Querying#FuzzySearch
     * @param fuzzy Decimal value between 0.0 and 1.0.
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
     * The field is the name of the field to sort using sorterName
     *
     * @param field field Field to use in order by
     * @param sorterName Sorter to use
     * @return Query instance
     */
    TSelf orderBy(String field, String sorterName);

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
    //TBD expr TSelf OrderBy<TValue>(Expression<Func<T, TValue>> propertySelector, string sorterName);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     * @param field Field to use in order by
     * @return Query instance
     */
    TSelf orderByDescending(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort using sorterName
     * @param field Field to use in order by
     * @param sorterName Sorter to use
     * @return Query instance
     */
    TSelf orderByDescending(String field, String sorterName);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by descending.
     * @param field Field to use in order by
     * @param ordering Ordering type
     * @return Query instance
     */
    TSelf orderByDescending(String field, OrderingType ordering);

    //TBD expr TSelf OrderByDescending<TValue>(params Expression<Func<T, TValue>>[] propertySelectors);
    //TBD expr TSelf OrderByDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string sorterName);

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

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @param roundFactor Round factor
     * @return Query instance
     */
    TSelf orderByDistance(String fieldName, double latitude, double longitude, double roundFactor);


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

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use in order by
     * @param latitude Latitude
     * @param longitude Longitude
     * @param roundFactor Round factor
     * @return Query instance
     */
    TSelf orderByDistanceDescending(String fieldName, double latitude, double longitude, double roundFactor);

    //TBD expr TSelf OrderByDistanceDescending<TValue>(Expression<Func<T, TValue>> propertySelector, string shapeWkt);

    /**
     * Sorts the query results by distance.
     * @param fieldName Field name to use
     * @param shapeWkt WKT shape to use
     * @return Query instance
     */
    TSelf orderByDistanceDescending(String fieldName, String shapeWkt);
}
