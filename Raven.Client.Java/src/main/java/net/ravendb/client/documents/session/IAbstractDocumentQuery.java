package net.ravendb.client.documents.session;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.GroupByMethod;
import net.ravendb.client.documents.queries.SearchOperator;
import net.ravendb.client.documents.queries.spatial.SpatialCriteria;
import net.ravendb.client.documents.queries.spatial.SpatialDynamicField;
import net.ravendb.client.primitives.Tuple;

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
     */
    DocumentConventions getConventions();

    /**
     * Determines if it is a dynamic map-reduce query
     */
    boolean isDynamicMapReduce();

    /**
     * Instruct the query to wait for non stale result for the specified wait timeout.
     */
    void _waitForNonStaleResults(Duration waitTimeout);

    /**
     * Gets the fields for projection
     */
    List<String> getProjectionFields();

    /**
     * Order the search results randomly
     */
    void _randomOrdering();

    /**
     * Order the search results randomly using the specified seed
     * this is useful if you want to have repeatable random queries
     */
    void _randomOrdering(String seed);

    /**
     * Sort using custom sorter on the server
     */
    //TBD void _customSortUsing(String typeName);

    /**
     * Sort using custom sorter on the server
     */
    //TBD void _customSortUsing(String typeName, boolean descending);

    /**
     * Includes the specified path in the query, loading the document specified in that path
     */
    void _include(String path);

    // TBD linq void Include(Expression<Func<T, object>> path);

    /**
     * Takes the specified count.
     */
    void _take(int count);

    /**
     * Skips the specified count.
     */
    void _skip(int count);

    /**
     * Matches value
     */
    void _whereEquals(String fieldName, Object value);

    /**
     * Matches value
     */
    void _whereEquals(String fieldName, Object value, boolean exact);

    /**
     * Matches value
     */
    void _whereEquals(WhereParams whereParams);

    /**
     * Not matches value
     */
    void _whereNotEquals(String fieldName, Object value);

    /**
     * Not matches value
     */
    void _whereNotEquals(String fieldName, Object value, boolean exact);

    /**
     * Not matches value
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
     */
    void _whereIn(String fieldName, Collection<Object> values);

    /**
     * Check that the field has one of the specified value
     */
    void _whereIn(String fieldName, Collection<Object> values, boolean exact);

    /**
     * Matches fields which starts with the specified value.
     */
    void _whereStartsWith(String fieldName, Object value);

    /**
     * Matches fields which ends with the specified value.
     */
    void _whereEndsWith(String fieldName, Object value);

    /**
     * Matches fields where the value is between the specified start and end, exclusive
     */
    void _whereBetween(String fieldName, Object start, Object end);

    /**
     * Matches fields where the value is between the specified start and end, exclusive
     */
    void _whereBetween(String fieldName, Object start, Object end, boolean exact);

    /**
     * Matches fields where the value is greater than the specified value
     */
    void _whereGreaterThan(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than the specified value
     */
    void _whereGreaterThan(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     */
    void _whereGreaterThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is greater than or equal to the specified value
     */
    void _whereGreaterThanOrEqual(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is less than the specified value
     */
    void _whereLessThan(String fieldName, Object value);

    /**
     * Matches fields where the value is less than the specified value
     */
    void _whereLessThan(String fieldName, Object value, boolean exact);

    /**
     * Matches fields where the value is less than or equal to the specified value
     */
    void _whereLessThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is less than or equal to the specified value
     */
    void _whereLessThanOrEqual(String fieldName, Object value, boolean exact);

    void _whereExists(String fieldName);

    void _whereRegex(String fieldName, String pattern);

    void _cmpXchg(String key, Object value);

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
     */
    void _boost(double boost);

    /**
     * Specifies a fuzziness factor to the single word term in the last where clause
     *
     * 0.0 to 1.0 where 1.0 means closer match
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
     */
    void _fuzzy(double fuzzy);

    /**
     * Specifies a proximity distance for the phrase in the last where clause
     *
     * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
     */
    void _proximity(int proximity);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     */
    void _orderBy(String field);

    /**
     * Order the results by the specified fields
     * The field is the name of the field to sort, defaulting to sorting by ascending.
     */
    void _orderBy(String field, OrderingType ordering);

    void _orderByDescending(String field);

    void _orderByDescending(String field, OrderingType ordering);

    void _orderByScore();

    void _orderByScoreDescending();

    //TBD void Highlight(string fieldName, int fragmentLength, int fragmentCount, string fragmentsField);
    //TBD void Highlight(string fieldName, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);
    //TBD void Highlight(string fieldName, string fieldKeyName, int fragmentLength, int fragmentCount, out FieldHighlightings highlightings);
    //TBD void SetHighlighterTags(string preTag, string postTag);
    //TBD void SetHighlighterTags(string[] preTags, string[] postTags);

    /**
     * EXPERT ONLY: Instructs the query to wait for non stale results.
     * This shouldn't be used outside of unit tests unless you are well aware of the implications
     */
    void _waitForNonStaleResults();

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     */
    void _search(String fieldName, String searchTerms);

    /**
     * Perform a search for documents which fields that match the searchTerms.
     * If there is more than a single term, each of them will be checked independently.
     */
    void _search(String fieldName, String searchTerms, SearchOperator operator);

    String toString();

    void _intersect();

    void _addRootType(Class clazz);

    void _distinct();

    /**
     * Performs a query matching ANY of the provided values against the given field (OR)
     */
    void _containsAny(String fieldName, Collection<Object> values);

    /**
     * Performs a query matching ALL of the provided values against the given field (AND)
     */
    void _containsAll(String fieldName, Collection<Object> values);

    void _groupBy(String fieldName, String... fieldNames);

    void _groupBy(Tuple<String, GroupByMethod> field, Tuple<String, GroupByMethod>... fields);

    void _groupByKey(String fieldName);

    void _groupByKey(String fieldName, String projectedName);

    void _groupBySum(String fieldName);

    void _groupBySum(String fieldName, String projectedName);

    void _groupByCount();

    void _groupByCount(String projectedName);

    void _whereTrue();

    void _spatial(SpatialDynamicField field, SpatialCriteria criteria);

    void _spatial(String fieldName, SpatialCriteria criteria);

    void _orderByDistance(String fieldName, double latitude, double longitude);

    void _orderByDistance(String fieldName, String shapeWkt);

    void _orderByDistanceDescending(String fieldName, double latitude, double longitude);

    void _orderByDistanceDescending(String fieldName, String shapeWkt);

    //TBD: MoreLikeThisScope MoreLikeThis();
    //TBD void AggregateBy(FacetBase facet);
    //TBD void AggregateUsing(string facetSetupDocumentKey);
    //TBD void AddFromAliasToWhereTokens(string fromAlias);
    //TBD string LoadParameter(object id);
    //TBD void SuggestUsing(SuggestionBase suggestion);

    Iterator<T> iterator();
}
