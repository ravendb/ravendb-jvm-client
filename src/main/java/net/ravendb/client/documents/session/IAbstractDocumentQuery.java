package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.GroupBy;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.queries.facets.FacetBase;
import net.ravendb.client.documents.queries.highlighting.HighlightingOptions;
import net.ravendb.client.documents.queries.highlighting.Highlightings;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisScope;
import net.ravendb.client.documents.queries.spatial.DynamicSpatialField;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.suggestions.SuggestionBase;
import net.ravendb.client.documents.session.loaders.IncludeBuilderBase;
import net.ravendb.client.primitives.Reference;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Mostly used by the linq provider
 */
public interface IAbstractDocumentQuery<T> {

    String getIndexName();

    String getCollectionName();

    /**
     * Gets the document convention from the query session
     * @return document conventions
     */
    DocumentConventions getConventions();

    /**
     * Determines if it is a dynamic map-reduce query
     * @return true if it is dynamic query
     */
    boolean isDynamicMapReduce();

    /**
     * Instruct the query to wait for non stale result for the specified wait timeout.
     * @param waitTimeout Wait timeout
     */
    void _waitForNonStaleResults(Duration waitTimeout);

    /**
     * Gets the fields for projection
     * @return list of projection fields
     */
    List<String> getProjectionFields();

    /**
     * Order the search results randomly
     */
    void _randomOrdering();

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     * @param seed Seed to use
     */
    void _randomOrdering(String seed);

    //TBD 4.1 void _customSortUsing(String typeName);

    //TBD 4.1 void _customSortUsing(String typeName, boolean descending);

    /**
     * Includes the specified path in the query, loading the document specified in that path
     * @param path include path
     */
    void _include(String path);

    /**
     * Includes the specified documents and/or counters in the query, specified by IncludeBuilder
     * @param includes builder
     */
    void _include(IncludeBuilderBase includes);

    // TBD expr linq void Include(Expression<Func<T, object>> path);

    /**
     * Takes the specified count.
     * @param count Items to take
     */
    void _take(int count);

    /**
     * Skips the specified count.
     * @param count Items to skip
     */
    void _skip(int count);

    /**
     * Matches value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereEquals(String fieldName, Object value);

    /**
     * Matches value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereEquals(String fieldName, Object value, boolean exact);

    /**
     * Matches value
     * @param fieldName Field name
     * @param method Method call to use
     */
    void _whereEquals(String fieldName, MethodCall method);

    /**
     * Matches value
     * @param fieldName Field name
     * @param method Method call to use
     * @param exact Use exact matcher
     */
    void _whereEquals(String fieldName, MethodCall method, boolean exact);

    /**
     * Matches value
     * @param whereParams Where parameters
     */
    void _whereEquals(WhereParams whereParams);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereNotEquals(String fieldName, Object value);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereNotEquals(String fieldName, Object value, boolean exact);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param method Method call to use
     */
    void _whereNotEquals(String fieldName, MethodCall method);

    /**
     * Not matches value
     * @param fieldName Field name
     * @param method Method call to use
     * @param exact Use exact matcher
     */
    void _whereNotEquals(String fieldName, MethodCall method, boolean exact);

    /**
     * Not matches value
     * @param whereParams Where parameters
     */
    void _whereNotEquals(WhereParams whereParams);

    /**
     * Simplified method for opening a new clause within the query
     */
    void _openSubclause();

    /**
     * Simplified method for closing a clause within the query
     */
    void _closeSubclause();

    /**
     * Negate the next operation
     */
    void negateNext();

    /**
     * Check that the field has one of the specified value
     * @param fieldName Field name
     * @param values Values to match
     */
    void _whereIn(String fieldName, Collection<Object> values);

    /**
     * Check that the field has one of the specified value
     * @param fieldName Field name
     * @param values Values to match
     * @param exact Use exact matcher
     */
    void _whereIn(String fieldName, Collection<Object> values, boolean exact);

    /**
     * Matches fields which starts with the specified value.
     * @param fieldName Field name
     * @param value to match
     */
    void _whereStartsWith(String fieldName, Object value);

    /**
     * Matches fields which starts with the specified value.
     * @param fieldName Field name
     * @param value to match
     * @param exact Use exact matcher
     */
    void _whereStartsWith(String fieldName, Object value, boolean exact);

    /**
     * Matches fields which ends with the specified value.
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereEndsWith(String fieldName, Object value);

    /**
     * Matches fields which ends with the specified value.
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereEndsWith(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is between the specified start and end, inclusive
     * @param fieldName Field name
     * @param start Range start
     * @param end Range end
     */
    void _whereBetween(String fieldName, Object start, Object end);

    /**
     * Matches fields where the value is between the specified start and end, inclusive
     * @param fieldName Field name
     * @param start Range start
     * @param end Range end
     * @param exact Use exact matcher
     */
    void _whereBetween(String fieldName, Object start, Object end, boolean exact);

    /**
     * Matches fields where the value is greater than the specified value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereGreaterThan(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than the specified value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereGreaterThan(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereGreaterThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereGreaterThanOrEqual(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is less than the specified value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereLessThan(String fieldName, Object value);

    /**
     * Matches fields where the value is less than the specified value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereLessThan(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is less than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to match
     */
    void _whereLessThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is less than or equal to the specified value
     * @param fieldName Field name
     * @param value Value to match
     * @param exact Use exact matcher
     */
    void _whereLessThanOrEqual(String fieldName, Object value, boolean exact);

    void _whereExists(String fieldName);

    void _whereRegex(String fieldName, String pattern);

    /**
     * Add an AND to the query
     */
    void _andAlso();

    /**
     * Add an OR to the query
     */
    void _orElse();

    /**
     * Specifies a boost weight to the last where clause.
     * The higher the boost factor, the more relevant the term will be.
     *
     * boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
     * @param boost Boost value
     */
    void _boost(double boost);

    /**
     * Specifies a fuzziness factor to the single word term in the last where clause
     *
     * 0.0 to 1.0 where 1.0 means closer match
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
     *
     * @param fuzzy Fuzzy value
     */
    void _fuzzy(double fuzzy);

    /**
     * Specifies a proximity distance for the phrase in the last search clause
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
     * @param proximity Proximity value
     */
    void _proximity(int proximity);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     * @param field Field to use
     */
    void _orderBy(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     * @param field Field to use
     * @param ordering Ordering type
     */
    void _orderBy(String field, OrderingType ordering);

    void _orderByDescending(String field);

    void _orderByDescending(String field, OrderingType ordering);

    void _orderByScore();

    void _orderByScoreDescending();

    void _highlight(String fieldName, int fragmentLength, int fragmentCount, HighlightingOptions options, Reference<Highlightings> highlightings);

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     * @param fieldName Field name
     * @param searchTerms Search terms
     */
    void _search(String fieldName, String searchTerms);

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     * @param fieldName Field name
     * @param searchTerms Search terms
     * @param operator Operator
     */
    void _search(String fieldName, String searchTerms, SearchOperator operator);

    String toString();

    void _intersect();

    void _addRootType(Class clazz);

    void _distinct();

    /**
     * Performs a query matching ANY of the provided values against the given field (OR)
     * @param fieldName Field name
     * @param values Values to match
     */
    void _containsAny(String fieldName, Collection<Object> values);

    /**
     * Performs a query matching ALL of the provided values against the given field (AND)
     * @param fieldName Field name
     * @param values Values to match
     */
    void _containsAll(String fieldName, Collection<Object> values);

    void _groupBy(String fieldName, String... fieldNames);

    void _groupBy(GroupBy field, GroupBy... fields);

    void _groupByKey(String fieldName);

    void _groupByKey(String fieldName, String projectedName);

    void _groupBySum(String fieldName);

    void _groupBySum(String fieldName, String projectedName);

    void _groupByCount();

    void _groupByCount(String projectedName);

    void _whereTrue();

    void _spatial(DynamicSpatialField field, SpatialCriteria criteria);

    void _spatial(String fieldName, SpatialCriteria criteria);

    void _orderByDistance(DynamicSpatialField field, double latitude, double longitude);

    void _orderByDistance(String fieldName, double latitude, double longitude);

    void _orderByDistance(DynamicSpatialField field, String shapeWkt);

    void _orderByDistance(String fieldName, String shapeWkt);

    void _orderByDistanceDescending(DynamicSpatialField field, double latitude, double longitude);

    void _orderByDistanceDescending(String fieldName, double latitude, double longitude);

    void _orderByDistanceDescending(DynamicSpatialField field, String shapeWkt);

    void _orderByDistanceDescending(String fieldName, String shapeWkt);

    void _aggregateBy(FacetBase facet);

    void _aggregateUsing(String facetSetupDocumentId);

    MoreLikeThisScope _moreLikeThis();

    String addAliasToCounterIncludesTokens(String fromAlias);

    void _suggestUsing(SuggestionBase suggestion);

    Iterator<T> iterator();
}
